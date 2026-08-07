package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TeacherRequest(
        @NotBlank(message = "{validation.not_blank}") String employeeNo,
        @NotBlank(message = "{validation.not_blank}") String firstName,
        String lastName,
        @Size(max = 120) String email,
        @Size(max = 30) String phone,
        String designation,
        String qualification,
        LocalDate joinDate,
        UUID userId,
        List<UUID> subjectIds,
        List<UUID> sectionIds
) {
}
