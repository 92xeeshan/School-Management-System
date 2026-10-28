package com.schoolms.leave.dto;

import com.schoolms.common.enums.StaffType;

import java.util.UUID;

public record StaffLeaveOptionDto(
        StaffType staffType,
        UUID staffId,
        String staffName,
        String employeeNo,
        UUID policyId,
        String policyName
) {
}
