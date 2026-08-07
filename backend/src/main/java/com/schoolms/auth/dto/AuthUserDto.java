package com.schoolms.auth.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AuthUserDto(
        UUID id,
        UUID schoolId,
        String username,
        String email,
        String firstName,
        String lastName,
        String displayName,
        String locale,
        List<String> roles,
        Set<String> permissions
) {
}
