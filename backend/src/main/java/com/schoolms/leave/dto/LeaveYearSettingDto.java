package com.schoolms.leave.dto;

import com.schoolms.leave.LeaveYearMode;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveYearSettingDto(
        UUID id,
        LeaveYearMode mode,
        LocalDate yearStart,
        LocalDate yearEnd
) {
}
