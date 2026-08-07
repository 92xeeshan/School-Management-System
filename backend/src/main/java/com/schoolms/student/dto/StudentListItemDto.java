package com.schoolms.student.dto;

import java.util.UUID;

public record StudentListItemDto(
        StudentDto student,
        UUID enrollmentId,
        UUID academicYearId,
        String className,
        String sectionName,
        Integer rollNumber
) {
}
