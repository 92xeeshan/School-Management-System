package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StreamRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(max = 80) String name,
        @Size(max = 20) String code,
        Integer sortOrder,
        String status
) {
}
