package com.schoolms.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ExamScheduleRequest(
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        @NotNull(message = "{validation.not_null}") UUID classId,
        @NotNull(message = "{validation.not_null}") UUID sectionId,
        @NotNull(message = "{validation.not_null}") UUID subjectId,
        @NotBlank(message = "{validation.not_blank}") String examTerm,
        @NotNull(message = "{validation.not_null}") LocalDate examDate,
        @NotNull(message = "{validation.not_null}") LocalTime startTime,
        @NotNull(message = "{validation.not_null}") LocalTime endTime,
        String room,
        @NotNull(message = "{validation.not_null}") BigDecimal maxMarks,
        @NotNull(message = "{validation.not_null}") BigDecimal passMarks,
        List<UUID> invigilatorIds,
        String status
) {
}
