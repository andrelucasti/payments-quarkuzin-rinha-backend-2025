package io.andrelucas.payments;

import io.quarkus.runtime.Startup;
import io.quarkus.virtual.threads.VirtualThreads;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Response;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Startup
@ApplicationScoped
public class PaymentConsumer {
    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);
    private final String consumerName;
    private final RedisAPI redisAPI;
    private final PaymentClient paymentClient;



    public PaymentConsumer(@ConfigProperty(name="app.consumer-name") final String consumerName,
                           final RedisAPI redisAPI,
                           final PaymentClient paymentClient) {

        this.consumerName = consumerName;
        this.redisAPI = redisAPI;


        this.paymentClient = paymentClient;
    }

    @PostConstruct
    void init(){
        consumer();
    }

    @VirtualThreads
    private void consumer(){
        List<String> args = List.of(
                "GROUP", "payments_group",
                consumerName,
                "BLOCK", "0",
                "STREAMS", "payments_stream",
                ">"
                );

        redisAPI.xreadgroup(args)
                .map(PaymentEvent::from)
                .onFailure()
                .invoke(err -> log.error("Error reading group", err))
                .repeat().indefinitely()
                .subscribe()
                .with(response -> {
                    response.forEach(event -> {

                        PaymentClient.PaymentClientRequest request = new PaymentClient.PaymentClientRequest(event.correlationId(), event.amount(), Instant.now());
                        paymentClient.processPayment(request);
                        ack(response);
                    });
                });
    }

    private void ack(List<PaymentEvent> events) {
       events.forEach(event -> {
           redisAPI.xack(List.of(
                   "payments_stream",
                   "payments_group",
                   event.id()
           ))
                   .onFailure()
                   .invoke(e -> log.error("Error ack event {}", event.id(), e))
                   .subscribe()
           .with(r -> log.info("event acked {}", event.id()));
       });
    }

    public record PaymentEvent(String id, String correlationId, BigDecimal amount) {

        public static List<PaymentEvent> from(Response response) {
            var events = new java.util.ArrayList<PaymentEvent>();

            for (Response stream : response) {
                Response messages = stream.get(1);

                for (Response msg : messages) {
                    String id = msg.get(0).toString();
                    Response fields = msg.get(1);

                    String correlationId = null;
                    BigDecimal amount = null;

                    for (int i = 0; i < fields.size(); i += 2) {
                        String key = fields.get(i).toString();
                        String value = fields.get(i + 1).toString();
                        switch (key) {
                            case "correlationId" -> correlationId = value;
                            case "amount" -> amount = new BigDecimal(value);
                        }
                    }

                    events.add(new PaymentEvent(id, correlationId, amount));
                }
            }

            return events;
        }
    }


}
