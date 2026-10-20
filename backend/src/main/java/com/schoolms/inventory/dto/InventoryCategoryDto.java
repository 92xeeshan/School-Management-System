package com.schoolms.inventory.dto;

import com.schoolms.inventory.InventoryCategory;

import java.util.UUID;

public record InventoryCategoryDto(
        UUID id,
        String name,
        String description
) {
    public static InventoryCategoryDto from(InventoryCategory category) {
        return new InventoryCategoryDto(category.getId(), category.getName(), category.getDescription());
    }
}
