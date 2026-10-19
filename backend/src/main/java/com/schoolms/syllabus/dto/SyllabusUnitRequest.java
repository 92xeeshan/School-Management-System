package com.schoolms.syllabus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SyllabusUnitRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(max = 200, message = "{validation.size}") String title,
        @Size(max = 1000, message = "{validation.size}") String description,
        Integer sortOrder,
        LocalDate targetCompletionDate
) {
}
