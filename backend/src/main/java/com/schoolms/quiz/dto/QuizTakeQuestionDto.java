package com.schoolms.quiz.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuizTakeQuestionDto(
        UUID questionId,
        String questionType,
        String prompt,
        List<QuestionOptionDto> options,
        BigDecimal marks,
        String response
) {
}
