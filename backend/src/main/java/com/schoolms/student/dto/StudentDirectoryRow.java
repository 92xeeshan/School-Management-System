package com.schoolms.student.dto;

import com.schoolms.common.enums.Gender;
import com.schoolms.common.enums.StudentStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Flat projection used by the student directory query. Combines a student row
 * with the resolved enrollment/class/section for the selected academic year.
 */
public record StudentDirectoryRow(
        UUID id,
        String admissionNo,
        String firstName,
        String lastName,
        Gender gender,
        StudentStatus status,
        LocalDate admissionDate,
        UUID enrollmentId,
        UUID academicYearId,
        Integer rollNumber,
        UUID sectionId,
        String sectionName,
        UUID classId,
        String className
) {
}
