package com.schoolms.quiz.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record QuizItemRequest(
        @NotNull(message = "{validation.not_null}") UUID questionId,
        Integer sortOrder,
        BigDecimal marks
) {
}
