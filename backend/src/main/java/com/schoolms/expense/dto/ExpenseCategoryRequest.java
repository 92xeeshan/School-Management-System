package com.schoolms.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExpenseCategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 30) String code,
        @Size(max = 300) String description
) {
}
