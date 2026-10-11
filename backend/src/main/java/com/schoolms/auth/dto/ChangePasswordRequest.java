package com.schoolms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "{validation.not_blank}") String currentPassword,
        @NotBlank(message = "{validation.not_blank}") @Size(min = 8, max = 72) String newPassword,
        @NotBlank(message = "{validation.not_blank}") String confirmPassword
) {
}
