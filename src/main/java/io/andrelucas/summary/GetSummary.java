package io.andrelucas.summary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.mutiny.redis.client.RedisAPI;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class GetSummary {
    private static final Logger log = LoggerFactory.getLogger(GetSummary.class);
    private final RedisAPI redisAPI;
    private final ObjectMapper objectMapper;

    public GetSummary(final RedisAPI redisAPI,
                      final ObjectMapper objectMapper) {

        this.redisAPI = redisAPI;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<PaymentSummaryResponse> summary(Instant from, Instant to) {
        long fromTimestamp = from != null ? from.toEpochMilli() : 0;
        long toTimestamp = to != null ? to.toEpochMilli() : Instant.now().toEpochMilli();

        return redisAPI.zrangebyscore(List.of("payments:timeline", String.valueOf(fromTimestamp), String.valueOf(toTimestamp)))
                .onItem()
                .transform(response -> fromJson(response.toString()))
                .onItem()
                .transform(this::convertToResponse)
                .onFailure()
                .invoke(e -> log.error("Error while fetching payments from redis", e))
                .subscribe().asCompletionStage();

    }

    private List<PaymentSummaryData> fromJson(final String responseJson) {
        try {
            return objectMapper.readValue(responseJson,
                    new TypeReference<List<PaymentSummaryData>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private PaymentSummaryResponse convertToResponse(List<PaymentSummaryData> paymentSummaryData) {

        List<PaymentSummaryData> defaultData = paymentSummaryData.stream()
                .filter(data -> "default".equals(data.processor()))
                .toList();

        double defaultTotalValue = defaultData.stream()
                .map(PaymentSummaryData::amount)
                .map(BigDecimal::doubleValue)
                .mapToDouble(Double::doubleValue)
                .sum();

        PaymentSummaryResponse.IntegrationSummary defaultSummary = new PaymentSummaryResponse.IntegrationSummary(defaultData.size(), defaultTotalValue);


        List<PaymentSummaryData> fallbackData = paymentSummaryData.stream()
                .filter(data -> "fallback".equals(data.processor()))
                .toList();

        double fallbackTotalValue = fallbackData.stream()
                .map(PaymentSummaryData::amount)
                .map(BigDecimal::doubleValue)
                .mapToDouble(Double::doubleValue)
                .sum();


        PaymentSummaryResponse.IntegrationSummary fallbackSummary = new PaymentSummaryResponse.IntegrationSummary(fallbackData.size(), fallbackTotalValue);

        return new PaymentSummaryResponse(defaultSummary, fallbackSummary);
    }
}
