package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReportCardStudentDto(
        UUID studentId,
        String studentName,
        String admissionNo,
        Integer rollNumber,
        String photoUrl,
        boolean ready,
        boolean published,
        BigDecimal percentage,
        String overallGrade
) {
}
