package com.schoolms.leave.dto;

import com.schoolms.common.enums.StaffType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record AdjustLeaveBalanceRequest(
        @NotNull StaffType staffType,
        @NotNull UUID staffId,
        @NotNull UUID leaveTypeId,
        @NotNull BigDecimal days,
        @NotBlank @Size(max = 500) String reason
) {
}
