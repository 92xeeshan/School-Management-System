package com.schoolms.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(min = 3, max = 60) String username,
        @Email(message = "{validation.invalid_email}") String email,
        @Size(max = 30) String phone,
        @NotBlank(message = "{validation.not_blank}") @Size(min = 8, max = 72) String password,
        @NotBlank(message = "{validation.not_blank}") @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        String locale,
        List<String> roleCodes
) {
}
