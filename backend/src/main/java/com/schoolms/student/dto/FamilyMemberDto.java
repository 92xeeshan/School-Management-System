package com.schoolms.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record FamilyMemberDto(
        UUID id,
        @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @Size(max = 100) String occupation,
        @Pattern(regexp = "^$|^\\d{10}$", message = "validation.invalid") String phone,
        @Email @Size(max = 120) String email,
        @Size(max = 120) String qualification,
        @Size(max = 20) String relationship,
        @Size(max = 500) String address,
        boolean primary
) {
}
