package com.schoolms.quiz.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizTakeDto(
        UUID attemptId,
        UUID quizId,
        String title,
        String instructions,
        int durationMinutes,
        Instant startedAt,
        Instant expiresAt,
        String status,
        List<QuizTakeQuestionDto> questions
) {
}
