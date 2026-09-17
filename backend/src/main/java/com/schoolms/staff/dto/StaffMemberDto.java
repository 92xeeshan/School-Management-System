package com.schoolms.staff.dto;

import com.schoolms.academics.TeacherProfile;
import com.schoolms.common.enums.EmploymentType;
import com.schoolms.common.enums.Gender;
import com.schoolms.common.enums.StaffType;
import com.schoolms.staff.NonTeachingStaff;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Unified view of a staff member regardless of teaching/non-teaching type.
 * Teaching-only fields are {@code null} for non-teaching staff.
 */
public record StaffMemberDto(
        UUID id,
        StaffType staffType,
        UUID userId,
        String employeeNo,
        String firstName,
        String lastName,
        String displayName,
        String email,
        String phone,
        Gender gender,
        LocalDate dateOfBirth,
        String designation,
        String department,
        String qualification,
        EmploymentType employmentType,
        LocalDate joinDate,
        String address,
        String status,
        List<UUID> subjectIds,
        List<SectionRefDto> classTeacherOf
) {

    public static StaffMemberDto from(TeacherProfile teacher, List<UUID> subjectIds,
                                      List<SectionRefDto> classTeacherOf) {
        return new StaffMemberDto(
                teacher.getId(), StaffType.TEACHING, teacher.getUserId(), teacher.getEmployeeNo(),
                teacher.getFirstName(), teacher.getLastName(), teacher.getDisplayName(),
                teacher.getEmail(), teacher.getPhone(), teacher.getGender(), teacher.getDateOfBirth(),
                teacher.getDesignation(), teacher.getDepartment(), teacher.getQualification(),
                teacher.getEmploymentType(), teacher.getJoinDate(), teacher.getAddress(),
                teacher.getStatus(), subjectIds, classTeacherOf);
    }

    public static StaffMemberDto from(NonTeachingStaff staff) {
        return new StaffMemberDto(
                staff.getId(), StaffType.NON_TEACHING, staff.getUserId(), staff.getEmployeeNo(),
                staff.getFirstName(), staff.getLastName(), staff.getDisplayName(),
                staff.getEmail(), staff.getPhone(), staff.getGender(), staff.getDateOfBirth(),
                staff.getDesignation(), staff.getDepartment(), staff.getQualification(),
                staff.getEmploymentType(), staff.getJoinDate(), staff.getAddress(),
                staff.getStatus(), null, null);
    }
}
