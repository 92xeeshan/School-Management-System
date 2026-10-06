package com.schoolms.payroll.dto;

import com.schoolms.common.enums.PayrollRunStatus;
import com.schoolms.common.enums.StaffType;
import com.schoolms.payroll.Payslip;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PayslipDto(
        UUID id,
        UUID payrollRunId,
        short year,
        short month,
        PayrollRunStatus runStatus,
        StaffType staffType,
        UUID staffId,
        UUID userId,
        String employeeNo,
        String staffName,
        String designation,
        String department,
        BigDecimal basic,
        BigDecimal hra,
        List<SalaryLineItemDto> allowances,
        List<SalaryLineItemDto> deductions,
        BigDecimal gross,
        BigDecimal totalDeductions,
        BigDecimal net
) {
    public static PayslipDto from(
            Payslip row,
            short year,
            short month,
            PayrollRunStatus runStatus,
            List<SalaryLineItemDto> allowances,
            List<SalaryLineItemDto> deductions) {
        return new PayslipDto(
                row.getId(),
                row.getPayrollRunId(),
                year,
                month,
                runStatus,
                row.getStaffType(),
                row.getStaffId(),
                row.getUserId(),
                row.getEmployeeNo(),
                row.getStaffName(),
                row.getDesignation(),
                row.getDepartment(),
                row.getBasic(),
                row.getHra(),
                allowances,
                deductions,
                row.getGross(),
                row.getTotalDeductions(),
                row.getNet());
    }
}
