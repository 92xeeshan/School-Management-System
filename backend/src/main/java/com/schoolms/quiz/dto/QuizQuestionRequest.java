package com.schoolms.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuizQuestionRequest(
        UUID subjectId,
        UUID classId,
        @NotBlank(message = "{validation.not_blank}") String questionType,
        @NotBlank(message = "{validation.not_blank}") String prompt,
        List<QuestionOptionDto> options,
        String correctAnswer,
        @NotNull(message = "{validation.not_null}") BigDecimal marks,
        String explanation
) {
}
