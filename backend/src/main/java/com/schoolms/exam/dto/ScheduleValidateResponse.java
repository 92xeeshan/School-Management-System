package com.schoolms.exam.dto;

import java.util.List;

public record ScheduleValidateResponse(
        boolean valid,
        List<ScheduleConflictDto> conflicts
) {
}
