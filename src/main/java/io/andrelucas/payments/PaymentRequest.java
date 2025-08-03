package io.andrelucas.payments;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
    @JsonProperty("correlationId") UUID correlationId, 
    @JsonProperty("amount") BigDecimal amount
) {
}
