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
@Table(name = "sibling_discount_rule")
@Getter
@Setter
public class SiblingDiscountRule extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "sibling_order", nullable = false)
    private short siblingOrder;

    @Column(name = "discount_type", nullable = false)
    private String discountType;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "fee_category_id", nullable = false)
    private UUID feeCategoryId;

    @Column(nullable = false)
    private String status = "ACTIVE";
}
