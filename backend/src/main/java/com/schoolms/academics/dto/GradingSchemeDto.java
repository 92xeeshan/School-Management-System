package com.schoolms.academics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GradingSchemeDto(
        UUID id,
        UUID academicYearId,
        String academicYearName,
        UUID classId,
        String className,
        String name,
        String academicLevel,
        String examType,
        String scaleType,
        BigDecimal passMarks,
        BigDecimal passPercent,
        BigDecimal maxMarks,
        String evaluationCriteria,
        String status,
        List<GradeBoundaryDto> boundaries,
        List<AssessmentWeightageDto> weightages
) {
}
