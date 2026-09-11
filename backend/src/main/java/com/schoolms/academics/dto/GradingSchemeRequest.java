package com.schoolms.academics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GradingSchemeRequest(
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        @NotNull(message = "{validation.not_null}") UUID classId,
        @NotBlank(message = "{validation.not_blank}") String name,
        @NotBlank(message = "{validation.not_blank}") String academicLevel,
        @NotBlank(message = "{validation.not_blank}") String examType,
        @NotBlank(message = "{validation.not_blank}") String scaleType,
        @NotNull(message = "{validation.not_null}") BigDecimal passMarks,
        @NotNull(message = "{validation.not_null}") BigDecimal passPercent,
        @NotNull(message = "{validation.not_null}") BigDecimal maxMarks,
        String evaluationCriteria,
        String status,
        @Valid List<GradeBoundaryRequest> boundaries,
        @Valid List<AssessmentWeightageRequest> weightages
) {
}
