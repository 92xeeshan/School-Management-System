package com.schoolms.academics.dto;

import com.schoolms.academics.TeacherProfile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TeacherDto(
        UUID id,
        UUID userId,
        String employeeNo,
        String firstName,
        String lastName,
        String displayName,
        String email,
        String phone,
        String designation,
        String qualification,
        LocalDate joinDate,
        String status,
        List<UUID> subjectIds,
        List<UUID> sectionIds
) {
    public static TeacherDto from(TeacherProfile teacher) {
        return new TeacherDto(
                teacher.getId(), teacher.getUserId(), teacher.getEmployeeNo(),
                teacher.getFirstName(), teacher.getLastName(), teacher.getDisplayName(),
                teacher.getEmail(), teacher.getPhone(), teacher.getDesignation(),
                teacher.getQualification(), teacher.getJoinDate(), teacher.getStatus(),
                null, null);
    }
}
