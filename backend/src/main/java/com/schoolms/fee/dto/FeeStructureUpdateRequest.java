package com.schoolms.fee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeStructureUpdateRequest(
        @NotNull(message = "{validation.not_null}") @Positive(message = "{validation.positive}") BigDecimal amount,
        String frequency,
        Short dueDay,
        LocalDate dueDate,
        LocalDate applicableFrom,
        LocalDate applicableTo
) {
}
