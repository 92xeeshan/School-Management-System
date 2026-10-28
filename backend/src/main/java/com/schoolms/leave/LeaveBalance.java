package com.schoolms.leave;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.StaffType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_balance")
@Getter
@Setter
public class LeaveBalance extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", nullable = false, length = 20)
    private StaffType staffType;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;

    @Column(name = "year_start", nullable = false)
    private LocalDate yearStart;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal entitled = BigDecimal.ZERO;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal used = BigDecimal.ZERO;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal pending = BigDecimal.ZERO;
}
