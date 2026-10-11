package com.schoolms.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordStartRequest(
        @NotBlank(message = "{validation.not_blank}") String username
) {
}
