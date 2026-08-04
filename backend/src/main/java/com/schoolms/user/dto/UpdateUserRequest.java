package com.schoolms.user.dto;

import com.schoolms.common.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email(message = "{validation.invalid_email}") String email,
        @Size(max = 30) String phone,
        @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        String locale,
        UserStatus status
) {
}
