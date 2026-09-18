package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ExamScheduleDto(
        UUID id,
        UUID academicYearId,
        String academicYearName,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        UUID subjectId,
        String subjectName,
        String examTerm,
        LocalDate examDate,
        LocalTime startTime,
        LocalTime endTime,
        String room,
        BigDecimal maxMarks,
        BigDecimal passMarks,
        String status,
        List<InvigilatorRef> invigilators
) {
    public record InvigilatorRef(UUID id, String displayName) {
    }
}
