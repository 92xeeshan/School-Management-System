package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SubjectDistributionDto(
        UUID subjectId,
        String subjectName,
        int studentCount,
        BigDecimal averagePercent
) {
}
