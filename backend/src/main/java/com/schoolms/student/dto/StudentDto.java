package com.schoolms.student.dto;

import com.schoolms.common.enums.Gender;
import com.schoolms.common.enums.StudentStatus;
import com.schoolms.student.Student;

import java.time.LocalDate;
import java.util.UUID;

public record StudentDto(
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
        LocalDate admissionDate,
        String photoUrl,
        StudentStatus status,
        UUID userId
) {
    public static StudentDto from(Student student) {
        return new StudentDto(
                student.getId(), student.getAdmissionNo(), student.getFirstName(),
                student.getLastName(), student.getDisplayName(), student.getDateOfBirth(),
                student.getGender(), student.getBloodGroup(), student.getReligion(),
                student.getNationality(), student.getAdmissionDate(), student.getPhotoUrl(),
                student.getStatus(), student.getUserId());
    }
}
