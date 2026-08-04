package com.schoolms.fee.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record FeeAssignmentRequest(
        @NotNull(message = "{validation.not_null}") UUID studentId,
        @NotNull(message = "{validation.not_null}") UUID feeStructureId,
        String discountType,
        BigDecimal discountAmount
) {
}
