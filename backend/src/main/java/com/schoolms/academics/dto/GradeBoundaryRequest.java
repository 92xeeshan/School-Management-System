package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GradeBoundaryRequest(
        @NotBlank(message = "{validation.not_blank}") String label,
        @NotNull(message = "{validation.not_null}") BigDecimal minPercent,
        @NotNull(message = "{validation.not_null}") BigDecimal maxPercent,
        BigDecimal gpaValue,
        Integer sortOrder
) {
}
