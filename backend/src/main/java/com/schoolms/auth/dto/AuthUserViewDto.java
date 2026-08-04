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
        String email
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
                view.getEmail());
    }
}
