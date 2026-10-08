package com.schoolms.fee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AdhocFeeLevyRequest(
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        @NotNull(message = "{validation.not_null}") UUID feeCategoryId,
        @NotNull(message = "{validation.not_null}") @Positive(message = "{validation.positive}") BigDecimal amount,
        @NotNull(message = "{validation.not_null}") LocalDate dueDate,
        @NotBlank(message = "{validation.not_blank}") String scope,
        UUID classId,
        UUID sectionId,
        UUID studentId,
        String remarks
) {
}
