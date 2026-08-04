package com.schoolms.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "{validation.not_blank}") String username,
        @NotBlank(message = "{validation.not_blank}") String password
) {
}
