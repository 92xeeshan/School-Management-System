package com.schoolms.leave.dto;

import com.schoolms.common.enums.StaffType;
import com.schoolms.leave.LeaveRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestDto(
        UUID id,
        StaffType staffType,
        UUID staffId,
        String staffName,
        String employeeNo,
        UUID leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal days,
        String reason,
        LeaveRequestStatus status,
        String supportingDocName,
        String rejectionReason,
        String cancelReason,
        Instant createdAt,
        Instant decidedAt,
        boolean canCancel,
        boolean canApprove,
        LeaveBalanceDto balance
) {
}
