package com.schoolms.leave.dto;

import com.schoolms.common.enums.StaffType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AssignLeavePolicyRequest(
        @NotNull UUID policyId,
        StaffType staffType,
        @NotEmpty List<UUID> staffIds
) {
}
