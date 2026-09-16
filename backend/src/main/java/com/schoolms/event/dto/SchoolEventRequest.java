package com.schoolms.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SchoolEventRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(max = 200) String title,
        @Size(max = 1000) String description,
        String eventType,
        @NotNull(message = "{validation.not_blank}") LocalDate startDate,
        LocalDate endDate,
        Boolean allDay,
        LocalTime startTime,
        LocalTime endTime,
        @Size(max = 200) String location,
        String visibilityScope,
        UUID classId,
        UUID sectionId
) {
}
