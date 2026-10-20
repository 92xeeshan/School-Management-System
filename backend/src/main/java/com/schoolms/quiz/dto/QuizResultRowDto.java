package com.schoolms.quiz.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuizResultRowDto(
        UUID studentId,
        String admissionNo,
        String studentName,
        Integer rollNumber,
        UUID attemptId,
        int attemptNo,
        String source,
        String status,
        Instant submittedAt,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal percentage,
        boolean passed
) {
}
