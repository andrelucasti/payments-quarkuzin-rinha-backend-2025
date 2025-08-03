package io.andrelucas.summary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PaymentSummaryResponse(
        @JsonProperty("default") IntegrationSummary defaultStatus,
        @JsonProperty("fallback") IntegrationSummary fallbackStatus) {

    public record IntegrationSummary(
            int totalRequests,
            double totalAmount) {}
}
