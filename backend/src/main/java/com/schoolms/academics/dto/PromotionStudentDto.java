package com.schoolms.academics.dto;

import com.schoolms.common.enums.PromotionOutcome;

import java.util.UUID;

public record PromotionStudentDto(
        UUID studentId,
        String admissionNo,
        String displayName,
        Integer rollNumber,
        String sourceClassName,
        String sourceSectionName,
        PromotionOutcome outcome,
        String reason
) {
}
