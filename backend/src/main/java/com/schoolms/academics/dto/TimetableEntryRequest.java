package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record TimetableEntryRequest(
        @NotNull(message = "{validation.not_null}") UUID sectionId,
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        @NotNull(message = "{validation.not_null}") Short dayOfWeek,
        @NotNull(message = "{validation.not_null}") Short periodNumber,
        @NotNull(message = "{validation.not_null}") LocalTime startTime,
        @NotNull(message = "{validation.not_null}") LocalTime endTime,
        UUID subjectId,
        UUID teacherId,
        String room
) {
}
