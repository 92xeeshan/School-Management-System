package com.schoolms.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record QuizSubmitRequest(
        @NotNull(message = "{validation.not_null}") @Valid List<Answer> answers
) {
    public record Answer(
            @NotNull(message = "{validation.not_null}") UUID questionId,
            String response
    ) {
    }
}
