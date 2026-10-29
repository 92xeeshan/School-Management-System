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
@Table(name = "leave_policy_item")
@Getter
@Setter
public class LeavePolicyItem extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;

    @Column(name = "yearly_quota", nullable = false, precision = 6, scale = 1)
    private BigDecimal yearlyQuota = BigDecimal.ZERO;
}
