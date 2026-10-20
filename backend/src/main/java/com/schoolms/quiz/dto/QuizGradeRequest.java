package com.schoolms.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuizGradeRequest(
        @NotNull(message = "{validation.not_null}") @Valid List<GradeRow> answers
) {
    public record GradeRow(
            @NotNull(message = "{validation.not_null}") UUID questionId,
            BigDecimal awardedMarks,
            String graderComment
    ) {
    }
}
