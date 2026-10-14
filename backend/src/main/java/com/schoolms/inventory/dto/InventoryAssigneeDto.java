package com.schoolms.inventory.dto;

import java.util.UUID;

public record InventoryAssigneeDto(
        UUID id,
        String name,
        String username
) {
}
