package com.schoolms.fee;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "adhoc_fee_levy")
@Getter
@Setter
public class AdhocFeeLevy extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "fee_category_id", nullable = false)
    private UUID feeCategoryId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private String scope;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "student_id")
    private UUID studentId;

    private String remarks;

    @Column(name = "assigned_count", nullable = false)
    private int assignedCount;

    @Column(name = "created_by")
    private UUID createdBy;
}
