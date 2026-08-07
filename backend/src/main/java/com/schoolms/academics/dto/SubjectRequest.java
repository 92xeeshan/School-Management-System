package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotBlank;

public record SubjectRequest(
        @NotBlank(message = "{validation.not_blank}") String name,
        String code,
        String type,
        String description
) {
}
