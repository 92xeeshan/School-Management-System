package com.schoolms.inventory.dto;

import com.schoolms.common.enums.InventoryCondition;
import com.schoolms.inventory.InventoryItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryItemDto(
        UUID id,
        UUID categoryId,
        String categoryName,
        String name,
        String sku,
        String unit,
        int quantity,
        int reorderThreshold,
        boolean lowStock,
        LocalDate purchaseDate,
        BigDecimal purchaseCost,
        String vendor,
        String location,
        InventoryCondition condition,
        UUID assignedToUserId,
        String assignedToName,
        String assignedRoom
) {
    public static InventoryItemDto from(InventoryItem item, String categoryName, String assignedToName) {
        return new InventoryItemDto(
                item.getId(),
                item.getCategoryId(),
                categoryName,
                item.getName(),
                item.getSku(),
                item.getUnit(),
                item.getQuantity(),
                item.getReorderThreshold(),
                item.isLowStock(),
                item.getPurchaseDate(),
                item.getPurchaseCost(),
                item.getVendor(),
                item.getLocation(),
                item.getCondition(),
                item.getAssignedToUserId(),
                assignedToName,
                item.getAssignedRoom());
    }
}
