package com.schoolms.exam.dto;

import java.util.List;
import java.util.UUID;

public record StudentTrendDto(
        UUID studentId,
        String studentName,
        String admissionNo,
        UUID academicYearId,
        String academicYearName,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        List<StudentTrendPointDto> points
) {
}
