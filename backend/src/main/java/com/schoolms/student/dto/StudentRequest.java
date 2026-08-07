package com.schoolms.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record StudentRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(max = 30) String admissionNo,
        @NotBlank(message = "{validation.not_blank}") @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @Past(message = "validation.invalid") LocalDate dateOfBirth,
        String gender,
        @Size(max = 5) String bloodGroup,
        @Size(max = 30) String religion,
        @Size(max = 60) String nationality,
        LocalDate admissionDate,
        UUID userId
) {
}
