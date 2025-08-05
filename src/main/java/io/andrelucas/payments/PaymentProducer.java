package io.andrelucas.payments;

import io.vertx.mutiny.redis.client.RedisAPI;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class PaymentProducer {
    private static final Logger log = LoggerFactory.getLogger(PaymentProducer.class);

    private final RedisAPI redisAPI;

    public PaymentProducer(RedisAPI redisAPI) {
        this.redisAPI = redisAPI;
    }

    public void create(final PaymentRequest paymentRequest) {
        final var requestedAt = Instant.now().toEpochMilli();
        final var args = List.of(
                "payments_stream",
                "*",
                "correlationId", paymentRequest.correlationId().toString(),
                "amount", paymentRequest.amount().toString(),
                "requestedAt", String.valueOf(requestedAt)
        );

        redisAPI.xadd(args)
                .onFailure()
                .invoke(e -> log.error("Error creating payments stream", e))
                .subscribe()
                .with(entryId -> log.debug("Successfully created payment on stream - ID:{} | correlationId: {}", entryId, paymentRequest.correlationId()));
    }
}
