package com.schoolms.syllabus.dto;

import com.schoolms.common.enums.SyllabusChapterStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SyllabusChapterRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(max = 200, message = "{validation.size}") String title,
        @Size(max = 1000, message = "{validation.size}") String description,
        Integer sortOrder,
        SyllabusChapterStatus status,
        LocalDate targetCompletionDate
) {
}
