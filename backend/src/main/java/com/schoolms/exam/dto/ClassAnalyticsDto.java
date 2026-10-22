package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ClassAnalyticsDto(
        UUID academicYearId,
        String academicYearName,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        String examTerm,
        int studentCount,
        int publishedCount,
        BigDecimal classAverage,
        BigDecimal passPercent,
        List<SubjectDistributionDto> subjectDistribution,
        List<TopPerformerDto> topPerformers
) {
}
