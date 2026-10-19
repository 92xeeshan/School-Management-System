package com.schoolms.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InventoryCategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 300) String description
) {
}
