package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReportCardSubjectDto(
        UUID subjectId,
        String subjectName,
        String subjectCode,
        BigDecimal theory,
        BigDecimal maxTheory,
        BigDecimal practical,
        BigDecimal maxPractical,
        BigDecimal assignment,
        BigDecimal maxAssignment,
        BigDecimal total,
        BigDecimal maxTotal,
        BigDecimal percentage,
        String grade,
        String remarks
) {
}
