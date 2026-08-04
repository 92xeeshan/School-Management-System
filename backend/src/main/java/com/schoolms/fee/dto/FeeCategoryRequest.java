package com.schoolms.fee.dto;

import jakarta.validation.constraints.NotBlank;

public record FeeCategoryRequest(
        @NotBlank(message = "{validation.not_blank}") String name,
        @NotBlank(message = "{validation.not_blank}") String code,
        String description
) {
}
