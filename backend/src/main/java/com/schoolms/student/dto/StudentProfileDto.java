package com.schoolms.student.dto;

import com.schoolms.attendance.dto.AttendanceSummaryDto;
import com.schoolms.common.enums.Gender;
import com.schoolms.common.enums.StudentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudentProfileDto(
        UUID id,
        String admissionNo,
        String firstName,
        String lastName,
        String displayName,
        LocalDate dateOfBirth,
        Gender gender,
        String bloodGroup,
        String religion,
        String nationality,
        String phone,
        String emergencyContact,
        String permanentAddress,
        String presentAddress,
        String previousSchool,
        String fatherName,
        String motherName,
        String fatherPhone,
        String motherPhone,
        LocalDate admissionDate,
        String photoUrl,
        StudentStatus status,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        Integer rollNumber,
        UUID academicYearId,
        String academicYearName,
        LocalDate enrollmentDate,
        AttendanceSummaryDto attendance,
        boolean canEdit,
        List<String> editableFields
) {
}
