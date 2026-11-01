package com.schoolms.website;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enquiry")
@Getter
@Setter
public class Enquiry extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "parent_name", nullable = false, length = 120)
    private String parentName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "class_name", length = 80)
    private String className;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnquiryStatus status = EnquiryStatus.NEW;

    @Column(length = 1000)
    private String notes;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
