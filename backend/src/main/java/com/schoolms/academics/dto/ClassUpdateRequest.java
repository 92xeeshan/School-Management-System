package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record ClassUpdateRequest(
        @NotBlank(message = "{validation.not_blank}") String name,
        String code,
        UUID sectionId,
        String sectionName,
        @Positive(message = "{validation.positive}") Integer capacity,
        String room,
        List<UUID> subjectIds,
        UUID classTeacherId
) {
}
