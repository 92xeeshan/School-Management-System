package com.schoolms.payroll;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.PayrollRunStatus;
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
@Table(name = "payroll_run")
@Getter
@Setter
public class PayrollRun extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false)
    private Short year;

    @Column(nullable = false)
    private Short month;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayrollRunStatus status = PayrollRunStatus.DRAFT;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "published_by")
    private UUID publishedBy;

    private String notes;
}
