package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AssessmentWeightageRequest(
        @NotBlank(message = "{validation.not_blank}") String assessmentType,
        @NotNull(message = "{validation.not_null}") BigDecimal weightPercent
) {
}
