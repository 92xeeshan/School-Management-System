package com.schoolms.quiz.dto;

import jakarta.validation.constraints.NotBlank;

public record QuizStatusRequest(
        @NotBlank(message = "{validation.not_blank}") String status
) {
}
