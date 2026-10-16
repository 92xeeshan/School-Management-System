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
        String phone,
        String emergencyContact,
        String permanentAddress,
        String presentAddress,
        String previousSchool,
        StudentStatus status,
        UUID userId,
        String username,
        String temporaryPassword
) {
    public static StudentDto from(Student student) {
        return from(student, null, null);
    }

    public static StudentDto from(Student student, String username, String temporaryPassword) {
        return new StudentDto(
                student.getId(), student.getAdmissionNo(), student.getFirstName(),
                student.getLastName(), student.getDisplayName(), student.getDateOfBirth(),
                student.getGender(), student.getBloodGroup(), student.getReligion(),
                student.getNationality(), student.getAdmissionDate(), student.getPhotoUrl(),
                student.getPhone(), student.getEmergencyContact(), student.getPermanentAddress(),
                student.getPresentAddress(), student.getPreviousSchool(),
                student.getStatus(), student.getUserId(), username, temporaryPassword);
    }

    /**
     * Lightweight variant for list/directory views where only identity and
     * directory-relevant fields are populated.
     */
    public static StudentDto summary(UUID id, String admissionNo, String firstName, String lastName,
                                     LocalDate admissionDate, Gender gender, StudentStatus status) {
        String display = lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName;
        return new StudentDto(id, admissionNo, firstName, lastName, display, null,
                gender, null, null, null, admissionDate, null, null, null, null,
                null, null, status, null, null, null);
    }
}
