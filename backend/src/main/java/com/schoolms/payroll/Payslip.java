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
import java.util.UUID;

@Entity
@Table(name = "payslip")
@Getter
@Setter
public class Payslip extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "payroll_run_id", nullable = false)
    private UUID payrollRunId;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", nullable = false, length = 20)
    private StaffType staffType;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "employee_no", nullable = false)
    private String employeeNo;

    @Column(name = "staff_name", nullable = false)
    private String staffName;

    private String designation;

    private String department;

    @Column(nullable = false)
    private BigDecimal basic;

    @Column(nullable = false)
    private BigDecimal hra = BigDecimal.ZERO;

    @Column(name = "allowances_json", nullable = false)
    private String allowancesJson = "[]";

    @Column(name = "deductions_json", nullable = false)
    private String deductionsJson = "[]";

    @Column(nullable = false)
    private BigDecimal gross;

    @Column(name = "total_deductions", nullable = false)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal net;
}
