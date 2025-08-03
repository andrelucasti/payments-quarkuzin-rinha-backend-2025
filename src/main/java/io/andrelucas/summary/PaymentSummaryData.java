package io.andrelucas.summary;


import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public record PaymentSummaryData(String correlationId, BigDecimal amount, String processor) {
}
