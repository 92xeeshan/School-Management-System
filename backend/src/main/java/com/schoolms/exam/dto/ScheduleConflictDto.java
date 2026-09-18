package com.schoolms.exam.dto;

import java.util.UUID;

public record ScheduleConflictDto(
        String field,
        String code,
        UUID conflictingScheduleId
) {
}
