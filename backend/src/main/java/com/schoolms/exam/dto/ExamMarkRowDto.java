package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamMarkRowDto(
        UUID studentId,
        String admissionNo,
        String studentName,
        Integer rollNumber,
        BigDecimal theory,
        BigDecimal practical,
        BigDecimal assignment,
        String attendanceStatus,
        String remarks,
        BigDecimal total,
        BigDecimal percentage,
        String gradeLabel,
        String status,
        boolean theoryInvalid,
        boolean practicalInvalid,
        boolean assignmentInvalid
) {
}
