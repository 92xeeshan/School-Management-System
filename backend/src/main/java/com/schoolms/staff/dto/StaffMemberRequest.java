package com.schoolms.staff.dto;

import com.schoolms.common.enums.EmploymentType;
import com.schoolms.common.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Create/update payload shared by teaching and non-teaching staff.
 */
public record StaffMemberRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(max = 30) String employeeNo,
        @NotBlank(message = "{validation.not_blank}") @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @Email(message = "{validation.invalid_email}") @Size(max = 120) String email,
        @Size(max = 30) String phone,
        Gender gender,
        LocalDate dateOfBirth,
        @Size(max = 100) String designation,
        @Size(max = 100) String department,
        @Size(max = 150) String qualification,
        EmploymentType employmentType,
        LocalDate joinDate,
        @Size(max = 255) String address,
        UUID userId
) {
}
