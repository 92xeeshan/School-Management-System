package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record SectionRequest(
        @NotNull(message = "{validation.not_null}") UUID classId,
        @NotBlank(message = "{validation.not_blank}") String name,
        @Positive(message = "validation.positive") Integer capacity
) {
}
