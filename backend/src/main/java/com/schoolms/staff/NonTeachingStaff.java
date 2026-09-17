package com.schoolms.staff;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.EmploymentType;
import com.schoolms.common.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Non-teaching staff record (administration, library, facilities, ...).
 * Teaching staff are stored in {@code teacher_profile}.
 */
@Entity
@Table(name = "non_teaching_staff")
@Getter
@Setter
public class NonTeachingStaff extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "employee_no", nullable = false)
    private String employeeNo;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String designation;

    private String department;

    private String qualification;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 20)
    private EmploymentType employmentType;

    @Column(name = "join_date")
    private LocalDate joinDate;

    private String address;

    @Column(nullable = false)
    private String status = "ACTIVE";

    public String getDisplayName() {
        return lastName == null || lastName.isBlank()
                ? firstName
                : firstName + " " + lastName;
    }
}
