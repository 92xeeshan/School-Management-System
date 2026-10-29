package com.schoolms.leave.dto;

import com.schoolms.leave.LeaveStaffCategory;

import java.util.List;
import java.util.UUID;

public record LeavePolicyDto(
        UUID id,
        String name,
        LeaveStaffCategory staffCategory,
        boolean active,
        List<LeavePolicyItemDto> items
) {
}
