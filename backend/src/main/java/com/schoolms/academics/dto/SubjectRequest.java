package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record SubjectRequest(
        @NotBlank(message = "{validation.not_blank}") String name,
        String code,
        String type,
        String description,
        @Positive(message = "{validation.positive}") Integer weeklyPeriods,
        Boolean practical,
        String status,
        List<UUID> classIds,
        UUID teacherId
) {
}
