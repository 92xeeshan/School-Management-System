package com.schoolms.exam.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record MarksheetExportRequest(
        @NotNull UUID academicYearId,
        @NotNull String examTerm,
        UUID classId,
        UUID sectionId,
        List<UUID> studentIds
) {
}
