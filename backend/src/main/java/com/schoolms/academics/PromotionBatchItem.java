package com.schoolms.academics;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.PromotionOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "promotion_batch_item")
@Getter
@Setter
public class PromotionBatchItem extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "source_enrollment_id")
    private UUID sourceEnrollmentId;

    @Column(name = "destination_enrollment_id")
    private UUID destinationEnrollmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionOutcome outcome;

    @Column(length = 200)
    private String reason;
}
