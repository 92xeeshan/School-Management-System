package com.schoolms.leave.dto;

import com.schoolms.leave.LeaveType;

import java.math.BigDecimal;
import java.util.UUID;

public record LeaveTypeDto(
        UUID id,
        String code,
        String name,
        BigDecimal yearlyQuota,
        boolean paid,
        boolean reasonMandatory,
        boolean active
) {
    public static LeaveTypeDto from(LeaveType type) {
        return new LeaveTypeDto(
                type.getId(),
                type.getCode(),
                type.getName(),
                type.getYearlyQuota(),
                type.isPaid(),
                type.isReasonMandatory(),
                type.isActive());
    }
}
