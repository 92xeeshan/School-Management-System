package com.schoolms.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GuardianRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @Size(max = 20) String relationship,
        @Size(max = 120) String email,
        @Size(max = 30) String phone,
        @Size(max = 100) String occupation,
        UUID userId
) {
}
