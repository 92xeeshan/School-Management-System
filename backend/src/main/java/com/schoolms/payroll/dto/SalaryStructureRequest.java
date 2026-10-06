package com.schoolms.payroll.dto;

import com.schoolms.common.enums.StaffType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SalaryStructureRequest(
        @NotNull StaffType staffType,
        @NotNull UUID staffId,
        @NotNull @DecimalMin("0.00") BigDecimal basic,
        @NotNull @DecimalMin("0.00") BigDecimal hra,
        @Valid List<SalaryLineItemDto> allowances,
        @Valid List<SalaryLineItemDto> deductions,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
