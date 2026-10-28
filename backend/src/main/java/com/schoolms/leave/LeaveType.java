package com.schoolms.leave;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "leave_type")
@Getter
@Setter
public class LeaveType extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "yearly_quota", nullable = false, precision = 6, scale = 1)
    private BigDecimal yearlyQuota = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean paid = true;

    @Column(name = "reason_mandatory", nullable = false)
    private boolean reasonMandatory = true;

    @Column(nullable = false)
    private boolean active = true;
}
