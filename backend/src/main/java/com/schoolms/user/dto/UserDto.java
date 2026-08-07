package com.schoolms.user.dto;

import com.schoolms.common.enums.UserStatus;
import com.schoolms.user.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserDto(
        UUID id,
        UUID schoolId,
        String username,
        String email,
        String phone,
        String firstName,
        String lastName,
        String displayName,
        String locale,
        UserStatus status,
        List<String> roles,
        Instant createdAt
) {
    public static UserDto from(User user, List<String> roles) {
        return new UserDto(
                user.getId(), user.getSchoolId(), user.getUsername(), user.getEmail(),
                user.getPhone(), user.getFirstName(), user.getLastName(), user.getDisplayName(),
                user.getLocale(), user.getStatus(), roles, user.getCreatedAt());
    }
}
