package com.schoolms.auth.dto;

import com.schoolms.auth.AuthUserView;
import com.schoolms.common.enums.UserStatus;

public record AuthUserViewDto(
        java.util.UUID id,
        java.util.UUID schoolId,
        String username,
        String passwordHash,
        UserStatus status,
        String locale,
        String firstName,
        String lastName,
        String email,
        boolean mustChangePassword,
        boolean questionsSet,
        int tokenVersion,
        int failedLoginAttempts,
        java.time.Instant lockedUntil
) {
    public static AuthUserViewDto from(AuthUserView view) {
        return new AuthUserViewDto(
                view.getId(),
                view.getSchoolId(),
                view.getUsername(),
                view.getPasswordHash(),
                UserStatus.valueOf(view.getStatus()),
                view.getLocale(),
                view.getFirstName(),
                view.getLastName(),
                view.getEmail(),
                Boolean.TRUE.equals(view.getMustChangePassword()),
                Boolean.TRUE.equals(view.getQuestionsSet()),
                view.getTokenVersion() == null ? 0 : view.getTokenVersion(),
                view.getFailedLoginAttempts() == null ? 0 : view.getFailedLoginAttempts(),
                view.getLockedUntil());
    }
}
