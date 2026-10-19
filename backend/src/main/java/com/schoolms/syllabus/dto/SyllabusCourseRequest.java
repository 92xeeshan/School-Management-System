package com.schoolms.syllabus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record SyllabusCourseRequest(
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        @NotNull(message = "{validation.not_null}") UUID classId,
        UUID sectionId,
        @NotNull(message = "{validation.not_null}") UUID subjectId,
        UUID teacherId,
        @NotBlank(message = "{validation.not_blank}") @Size(max = 200, message = "{validation.size}") String title,
        @Size(max = 1000, message = "{validation.size}") String description,
        LocalDate targetCompletionDate,
        String status
) {
}
