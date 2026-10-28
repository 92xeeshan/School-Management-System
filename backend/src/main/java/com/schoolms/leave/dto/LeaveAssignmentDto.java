package com.schoolms.leave.dto;

import com.schoolms.common.enums.StaffType;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveAssignmentDto(
        UUID id,
        StaffType staffType,
        UUID staffId,
        String staffName,
        String employeeNo,
        UUID policyId,
        String policyName,
        LocalDate yearStart
) {
}
