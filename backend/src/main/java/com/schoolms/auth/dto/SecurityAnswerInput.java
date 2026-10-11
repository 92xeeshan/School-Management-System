package com.schoolms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SecurityAnswerInput(
        @NotNull UUID questionId,
        @NotBlank(message = "{validation.not_blank}") String answer
) {
}
