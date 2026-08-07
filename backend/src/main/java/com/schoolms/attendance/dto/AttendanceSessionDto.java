package com.schoolms.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceSessionDto(
        UUID id,
        UUID sectionId,
        UUID academicYearId,
        LocalDate attendanceDate,
        UUID subjectId,
        String status,
        UUID markedBy,
        Instant markedAt
) {
}
