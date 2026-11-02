package com.schoolms.website.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record PublicEventDto(
        UUID id,
        String title,
        String description,
        String eventType,
        LocalDate startDate,
        LocalDate endDate,
        boolean allDay,
        LocalTime startTime,
        LocalTime endTime,
        String location
) {
}
