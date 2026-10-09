package com.schoolms.fee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fee_structure_audit")
@Getter
@Setter
public class FeeStructureAudit {

    @Id
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "fee_structure_id")
    private UUID feeStructureId;

    @Column(name = "academic_year_id")
    private UUID academicYearId;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false)
    private String action;

    @Column(name = "previous_amount")
    private BigDecimal previousAmount;

    @Column(name = "new_amount")
    private BigDecimal newAmount;

    @Column(name = "previous_frequency")
    private String previousFrequency;

    @Column(name = "new_frequency")
    private String newFrequency;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
