package com.schoolms.auth.dto;

import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Size(min = 8, max = 72) String temporaryPassword
) {
}
