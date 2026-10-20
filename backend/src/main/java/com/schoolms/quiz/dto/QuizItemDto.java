package com.schoolms.quiz.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QuizItemDto(
        UUID id,
        UUID questionId,
        int sortOrder,
        BigDecimal marks,
        QuizQuestionDto question
) {
}
