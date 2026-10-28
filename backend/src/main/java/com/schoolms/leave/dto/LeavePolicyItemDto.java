package com.schoolms.leave.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LeavePolicyItemDto(
        UUID leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        BigDecimal yearlyQuota
) {
}
