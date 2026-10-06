package com.schoolms.payroll;

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
@Table(name = "staff_salary_structure")
@Getter
@Setter
public class StaffSalaryStructure extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", nullable = false, length = 20)
    private StaffType staffType;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(nullable = false)
    private BigDecimal basic;

    @Column(nullable = false)
    private BigDecimal hra = BigDecimal.ZERO;

    @Column(name = "allowances_json", nullable = false)
    private String allowancesJson = "[]";

    @Column(name = "deductions_json", nullable = false)
    private String deductionsJson = "[]";

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private String status = "ACTIVE";
}
