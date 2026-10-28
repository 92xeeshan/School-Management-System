package com.schoolms.leave.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LeaveTypeRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 80) String name,
        @NotNull @DecimalMin("0") BigDecimal yearlyQuota,
        @NotNull Boolean paid,
        @NotNull Boolean reasonMandatory,
        Boolean active
) {
}
