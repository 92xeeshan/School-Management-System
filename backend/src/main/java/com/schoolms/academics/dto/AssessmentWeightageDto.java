package com.schoolms.academics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AssessmentWeightageDto(
        UUID id,
        String assessmentType,
        BigDecimal weightPercent
) {
}
