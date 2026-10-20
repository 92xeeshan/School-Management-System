package com.schoolms.quiz.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizQuestionDto(
        UUID id,
        UUID subjectId,
        String subjectName,
        UUID classId,
        String className,
        String questionType,
        String prompt,
        List<QuestionOptionDto> options,
        String correctAnswer,
        BigDecimal marks,
        String explanation,
        Instant createdAt
) {
}
