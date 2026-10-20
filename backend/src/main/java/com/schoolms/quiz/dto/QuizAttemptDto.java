package com.schoolms.quiz.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizAttemptDto(
        UUID id,
        UUID quizId,
        String quizTitle,
        UUID studentId,
        String studentName,
        String admissionNo,
        Integer rollNumber,
        int attemptNo,
        String status,
        Instant startedAt,
        Instant submittedAt,
        Instant expiresAt,
        BigDecimal autoScore,
        BigDecimal manualScore,
        BigDecimal totalScore,
        BigDecimal maxScore,
        BigDecimal percentage,
        boolean passed,
        List<QuizAnswerDto> answers
) {
}
