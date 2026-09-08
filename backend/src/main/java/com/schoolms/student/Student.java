package com.schoolms.student;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.Gender;
import com.schoolms.common.enums.StudentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student")
@Getter
@Setter
public class Student extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "admission_no", nullable = false)
    private String admissionNo;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "blood_group")
    private String bloodGroup;

    private String religion;

    private String nationality;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(length = 30)
    private String phone;

    @Column(name = "emergency_contact", length = 30)
    private String emergencyContact;

    @Column(name = "permanent_address", length = 500)
    private String permanentAddress;

    @Column(name = "present_address", length = 500)
    private String presentAddress;

    @Column(name = "previous_school", length = 200)
    private String previousSchool;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status = StudentStatus.ACTIVE;

    public String getDisplayName() {
        return lastName == null || lastName.isBlank()
                ? firstName
                : firstName + " " + lastName;
    }
}
