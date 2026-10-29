package com.schoolms.leave.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record LeavePolicyItemRequest(
        @NotNull UUID leaveTypeId,
        @NotNull @DecimalMin("0") BigDecimal yearlyQuota
) {
}
