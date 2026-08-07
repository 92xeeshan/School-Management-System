package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotBlank;

public record ClassRequest(
        @NotBlank(message = "{validation.not_blank}") String name,
        String code,
        Integer sortOrder
) {
}
