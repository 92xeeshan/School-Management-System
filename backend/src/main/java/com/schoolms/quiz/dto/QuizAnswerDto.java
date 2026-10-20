package com.schoolms.quiz.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuizAnswerDto(
        UUID questionId,
        String questionType,
        String prompt,
        List<QuestionOptionDto> options,
        String correctAnswer,
        String response,
        Boolean autoCorrect,
        BigDecimal autoMarks,
        BigDecimal awardedMarks,
        BigDecimal maxMarks,
        String graderComment
) {
}
