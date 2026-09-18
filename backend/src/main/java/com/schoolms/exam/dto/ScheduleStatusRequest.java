package com.schoolms.exam.dto;

import jakarta.validation.constraints.NotBlank;

public record ScheduleStatusRequest(
        @NotBlank(message = "{validation.not_blank}") String status
) {
}
