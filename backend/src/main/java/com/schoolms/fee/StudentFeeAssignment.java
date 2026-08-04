package com.schoolms.fee;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "student_fee_assignment")
@Getter
@Setter
public class StudentFeeAssignment extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "fee_structure_id", nullable = false)
    private UUID feeStructureId;

    @Column(name = "discount_type")
    private String discountType;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private String status = "ACTIVE";
}
