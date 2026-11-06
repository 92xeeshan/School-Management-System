package com.schoolms.student;

import com.schoolms.common.enums.AdmissionStepStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_step_status")
@Getter
@Setter
public class StudentStepStatus {

    @Id
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "step_no", nullable = false)
    private int stepNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdmissionStepStatus status = AdmissionStepStatus.PENDING;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
