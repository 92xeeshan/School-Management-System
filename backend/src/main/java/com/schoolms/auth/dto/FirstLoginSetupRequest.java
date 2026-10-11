package com.schoolms.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FirstLoginSetupRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(min = 8, max = 72) String newPassword,
        @NotBlank(message = "{validation.not_blank}") String confirmPassword,
        @NotEmpty @Valid List<SecurityAnswerInput> answers
) {
}
