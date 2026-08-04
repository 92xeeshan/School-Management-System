package com.schoolms.fee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FeeStructureRequest(
        @NotNull(message = "{validation.not_null}") UUID classId,
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        @NotNull(message = "{validation.not_null}") UUID categoryId,
        @NotNull(message = "{validation.not_null}") @Positive(message = "{validation.positive}") BigDecimal amount,
        String frequency,
        Short dueDay,
        LocalDate applicableFrom,
        LocalDate applicableTo
) {
}
