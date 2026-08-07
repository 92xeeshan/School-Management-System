package com.schoolms.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(max = 50) String code,
        @NotBlank(message = "{validation.not_blank}") @Size(max = 200) String name,
        @Size(max = 500) String address,
        @Size(max = 50) String phone,
        @Email(message = "{validation.invalid_email}") @Size(max = 120) String email,
        @Size(max = 10) String currency,
        @Size(max = 10) String defaultLocale,
        @Size(max = 60) String timezone,
        Short academicStartMonth
) {
}
