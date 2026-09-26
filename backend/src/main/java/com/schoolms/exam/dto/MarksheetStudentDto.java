package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MarksheetStudentDto(
        UUID studentId,
        String studentName,
        String admissionNo,
        Integer rollNumber,
        String gender,
        String photoUrl,
        boolean ready,
        String status,
        boolean published,
        boolean locked,
        Integer classRank,
        String rejectionReason,
        BigDecimal percentage,
        BigDecimal gpa,
        String overallGrade,
        String result
) {
}
