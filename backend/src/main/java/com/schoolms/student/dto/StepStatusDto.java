package com.schoolms.student.dto;

import com.schoolms.common.enums.AdmissionStepStatus;
import com.schoolms.student.StudentStepStatus;

import java.time.Instant;

public record StepStatusDto(
        int stepNo,
        AdmissionStepStatus status,
        Instant updatedAt
) {
    public static StepStatusDto from(StudentStepStatus row) {
        return new StepStatusDto(row.getStepNo(), row.getStatus(), row.getUpdatedAt());
    }
}
