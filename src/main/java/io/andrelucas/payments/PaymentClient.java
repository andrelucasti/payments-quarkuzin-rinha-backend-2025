package io.andrelucas.payments;

import io.quarkus.virtual.threads.VirtualThreads;
import io.vertx.mutiny.redis.client.RedisAPI;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ApplicationScoped
public class PaymentClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);
    private final HttpClient httpClient;

    private final String defaultBaseUri;
    private final String fallbackBaseUri;
    private final RedisAPI redisAPI;
    private final Executor executor;

    public PaymentClient(@ConfigProperty(name = "payment-processor.default.base-url") final String defaultBaseUri,
                         @ConfigProperty(name = "payment-processor.fallback.base-url") final String fallbackBaseUri,
                         final RedisAPI redisAPI,
                         @VirtualThreads final Executor executor) {

        this.defaultBaseUri = defaultBaseUri;
        this.fallbackBaseUri = fallbackBaseUri;
        this.redisAPI = redisAPI;
        this.executor = executor;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .executor(executor)
                .build();
    }

    public void processPayment(final PaymentClientRequest paymentRequest) {
        CompletableFuture.runAsync(() -> {
            try{
                sendDefault(paymentRequest);
            } catch (Exception e) {
                log.error("Got an error at send payment", e);
                try {
                    sendFallback(paymentRequest);
                } catch (Exception fallbackEx) {
                    log.error("Got an error at send payment to fallback", fallbackEx);
                }
            }
        },  executor);
    }

    private String toJson(final PaymentClientRequest paymentRequest) {
        return "{" +
                "\"correlationId\":\"" + paymentRequest.correlationId() + "\"," +
                "\"amount\":" + paymentRequest.amount().toString() + "," +
                "\"requestedAt\":\"" + DateTimeFormatter.ISO_INSTANT.format(paymentRequest.requestedAt()) + "\"" +
                "}";
    }

    private String toData(final PaymentClientRequest paymentRequest, final String processorType) {
        return "{" +
                "\"correlationId\":\"" + paymentRequest.correlationId() + "\"," +
                "\"amount\":" + paymentRequest.amount().toString() + "," +
                "\"processor\":\"" + processorType + "\"," +
                "\"requestedAt\":\"" + DateTimeFormatter.ISO_INSTANT.format(paymentRequest.requestedAt()) + "\"" +
                "}";
    }


    private void sendDefault(final PaymentClientRequest paymentRequest) throws IOException, InterruptedException {
        String payload = toJson(paymentRequest);
        log.info("Sending payment to default {}", payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(defaultBaseUri + "/payments"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5)) // Add request timeout
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> httpResponse = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (httpResponse.statusCode() == 200) {
            String data = toData(paymentRequest, "default");
            redisAPI.zadd(List.of("payments:timeline", String.valueOf(paymentRequest.requestedAt().toEpochMilli()), data))
                    .onFailure()
                    .invoke(e -> log.error("Got an error at to save payment", e))
                    .subscribe()
                    .with( r -> log.info(r.toString()));
        } else {
            log.error("Got an error at to save payment status code: {}, body: {}", httpResponse.statusCode(), httpResponse.body());
            throw new RuntimeException("Got an error at to send payment");
        }

    }

    private void sendFallback(final PaymentClientRequest paymentRequest) throws IOException, InterruptedException {
        String payload = toJson(paymentRequest);
        log.info("Sending payment to fallback {}", payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fallbackBaseUri + "/payments"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5)) // Add request timeout
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        String data = toData(paymentRequest, "fallback");
        redisAPI.zadd(List.of("payments:timeline", String.valueOf(paymentRequest.requestedAt().toEpochMilli()), data))
                .onFailure()
                .invoke(e -> log.error("Got an error at to save payment", e))
                .subscribe()
                .with( r -> log.info(r.toString()));

    }
    public record PaymentClientRequest(String correlationId, BigDecimal amount, Instant requestedAt) {}

}
