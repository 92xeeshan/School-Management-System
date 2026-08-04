package com.schoolms.fee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRequest(
        @NotNull(message = "{validation.not_null}") UUID studentId,
        UUID studentFeeAssignmentId,
        @NotNull(message = "{validation.not_null}") @Positive(message = "{validation.positive}") BigDecimal amountPaid,
        Instant paidAt,
        String paymentMethod,
        String referenceNo,
        String remarks
) {
}
