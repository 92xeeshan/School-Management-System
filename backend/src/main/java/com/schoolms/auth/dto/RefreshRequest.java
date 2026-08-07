package com.schoolms.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "{validation.not_blank}") String refreshToken
) {
}
