package com.schoolms.payroll.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SalaryLineItemDto(
        @NotBlank String name,
        @NotNull @DecimalMin("0.00") BigDecimal amount
) {
}
