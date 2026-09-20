package com.schoolms.exam.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AdmitCardSlotDto(
        UUID scheduleId,
        String subjectName,
        LocalDate examDate,
        LocalTime startTime,
        LocalTime endTime,
        String room
) {
}
