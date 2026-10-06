package com.schoolms.payroll.dto;

import com.schoolms.common.enums.StaffType;
import com.schoolms.payroll.StaffSalaryStructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SalaryStructureDto(
        UUID id,
        StaffType staffType,
        UUID staffId,
        String staffName,
        String employeeNo,
        BigDecimal basic,
        BigDecimal hra,
        List<SalaryLineItemDto> allowances,
        List<SalaryLineItemDto> deductions,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String status,
        BigDecimal gross,
        BigDecimal totalDeductions,
        BigDecimal net
) {
    public static SalaryStructureDto from(
            StaffSalaryStructure row,
            String staffName,
            String employeeNo,
            List<SalaryLineItemDto> allowances,
            List<SalaryLineItemDto> deductions,
            BigDecimal gross,
            BigDecimal totalDeductions,
            BigDecimal net) {
        return new SalaryStructureDto(
                row.getId(),
                row.getStaffType(),
                row.getStaffId(),
                staffName,
                employeeNo,
                row.getBasic(),
                row.getHra(),
                allowances,
                deductions,
                row.getEffectiveFrom(),
                row.getEffectiveTo(),
                row.getStatus(),
                gross,
                totalDeductions,
                net);
    }
}
