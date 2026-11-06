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

    @Column(name = "middle_name")
    private String middleName;

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

    @Column(name = "aadhaar_no", length = 12)
    private String aadhaarNo;

    @Column(name = "apaar_id", length = 12)
    private String apaarId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "religion_id")
    private UUID religionId;

    @Column(name = "mother_tongue_id")
    private UUID motherTongueId;

    @Column(name = "house_id")
    private UUID houseId;

    @Column(name = "medium_id")
    private UUID mediumId;

    @Column(name = "stream_id")
    private UUID streamId;

    @Column(length = 120)
    private String email;

    @Column(name = "medical_notes", length = 1000)
    private String medicalNotes;

    @Column(name = "transport_required", nullable = false)
    private boolean transportRequired;

    @Column(name = "hostel_required", nullable = false)
    private boolean hostelRequired;

    @Column(length = 1000)
    private String remarks;

    @Column(name = "emergency_name", length = 120)
    private String emergencyName;

    @Column(name = "emergency_relation", length = 40)
    private String emergencyRelation;

    @Column(name = "profile_pending", nullable = false)
    private boolean profilePending = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status = StudentStatus.ACTIVE;

    public String getDisplayName() {
        StringBuilder name = new StringBuilder(firstName);
        if (middleName != null && !middleName.isBlank()) {
            name.append(' ').append(middleName);
        }
        if (lastName != null && !lastName.isBlank()) {
            name.append(' ').append(lastName);
        }
        return name.toString();
    }
}
