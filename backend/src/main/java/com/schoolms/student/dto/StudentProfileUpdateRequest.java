package com.schoolms.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record StudentProfileUpdateRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @Past(message = "validation.invalid") LocalDate dateOfBirth,
        String gender,
        @Size(max = 5) String bloodGroup,
        @Size(max = 30) String religion,
        @Size(max = 60) String nationality,
        @Size(max = 30) String phone,
        @Size(max = 30) String emergencyContact,
        @Size(max = 500) String permanentAddress,
        @Size(max = 500) String presentAddress,
        @Size(max = 200) String previousSchool,
        @Size(max = 120) String fatherName,
        @Size(max = 120) String motherName,
        @Size(max = 30) String fatherPhone,
        @Size(max = 30) String motherPhone,
        LocalDate admissionDate,
        UUID classId,
        UUID sectionId,
        Integer rollNumber
) {
}
