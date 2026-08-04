package com.schoolms.academics.dto;

import java.time.LocalTime;
import java.util.UUID;

public record TimetableEntryDto(
        UUID id,
        UUID sectionId,
        String sectionName,
        String className,
        UUID academicYearId,
        Short dayOfWeek,
        Short periodNumber,
        LocalTime startTime,
        LocalTime endTime,
        UUID subjectId,
        String subjectName,
        UUID teacherId,
        String teacherName,
        String room
) {
}
