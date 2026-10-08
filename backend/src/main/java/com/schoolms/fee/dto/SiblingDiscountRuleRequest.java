package com.schoolms.fee.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record SiblingDiscountRuleRequest(
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        @Min(value = 2, message = "{validation.min}") short siblingOrder,
        @NotBlank(message = "{validation.not_blank}") String discountType,
        @NotNull(message = "{validation.not_null}") @Positive(message = "{validation.positive}") BigDecimal discountValue,
        @NotNull(message = "{validation.not_null}") UUID feeCategoryId,
        String status
) {
}
