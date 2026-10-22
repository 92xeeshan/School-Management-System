package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopPerformerDto(
        int rank,
        UUID studentId,
        String studentName,
        String admissionNo,
        Integer rollNumber,
        BigDecimal percentage,
        String overallGrade,
        String result
) {
}
