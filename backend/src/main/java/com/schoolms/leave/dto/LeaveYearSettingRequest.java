package com.schoolms.leave.dto;

import com.schoolms.leave.LeaveYearMode;
import jakarta.validation.constraints.NotNull;

public record LeaveYearSettingRequest(
        @NotNull LeaveYearMode mode
) {
}
