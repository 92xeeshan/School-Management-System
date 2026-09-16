package com.schoolms.event.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SchoolEventDto(
        UUID id,
        String title,
        String description,
        String eventType,
        LocalDate startDate,
        LocalDate endDate,
        boolean allDay,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        String visibilityScope,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName
) {
}
