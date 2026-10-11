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
        boolean mustChangePassword,
        boolean questionsSet,
        List<String> roles,
        Instant createdAt,
        Instant lastLoginAt,
        String temporaryPassword
) {
    public static UserDto from(User user, List<String> roles) {
        return from(user, roles, null);
    }

    public static UserDto from(User user, List<String> roles, String temporaryPassword) {
        return new UserDto(
                user.getId(), user.getSchoolId(), user.getUsername(), user.getEmail(),
                user.getPhone(), user.getFirstName(), user.getLastName(), user.getDisplayName(),
                user.getLocale(), user.getStatus(), user.isMustChangePassword(), user.isQuestionsSet(),
                roles, user.getCreatedAt(), user.getLastLoginAt(), temporaryPassword);
    }
}
