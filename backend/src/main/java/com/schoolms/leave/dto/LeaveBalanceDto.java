package com.schoolms.leave.dto;

import com.schoolms.common.enums.StaffType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveBalanceDto(
        UUID leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        boolean paid,
        StaffType staffType,
        UUID staffId,
        String staffName,
        LocalDate yearStart,
        BigDecimal entitled,
        BigDecimal used,
        BigDecimal pending,
        BigDecimal available
) {
}
