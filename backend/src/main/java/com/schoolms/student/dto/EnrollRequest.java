package com.schoolms.student.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EnrollRequest(
        @NotNull(message = "{validation.not_null}") UUID classId,
        @NotNull(message = "{validation.not_null}") UUID sectionId,
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        Integer rollNumber
) {
}
