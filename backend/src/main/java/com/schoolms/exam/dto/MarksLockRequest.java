package com.schoolms.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MarksLockRequest(
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        @NotNull(message = "{validation.not_null}") UUID classId,
        @NotNull(message = "{validation.not_null}") UUID sectionId,
        @NotNull(message = "{validation.not_null}") UUID subjectId,
        @NotBlank(message = "{validation.not_blank}") String examTerm,
        boolean locked,
        LocalDate entryDeadline
) {
}
