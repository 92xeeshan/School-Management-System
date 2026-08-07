package com.schoolms.auth.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        AuthUserDto user
) {
}
