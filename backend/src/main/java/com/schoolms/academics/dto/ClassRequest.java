package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record ClassRequest(
        @NotBlank(message = "{validation.not_blank}") String name,
        String code,
        Integer sortOrder,
        String sectionName,
        @Positive(message = "validation.positive") Integer capacity,
        List<UUID> subjectIds,
        UUID classTeacherId
) {
}
