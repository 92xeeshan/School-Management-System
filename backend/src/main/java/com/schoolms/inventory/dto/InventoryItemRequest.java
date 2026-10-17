package com.schoolms.inventory.dto;

import com.schoolms.common.enums.InventoryCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryItemRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 60) String sku,
        @Size(max = 30) String unit,
        @Min(0) Integer quantity,
        @Min(0) Integer reorderThreshold,
        LocalDate purchaseDate,
        @DecimalMin("0.00") BigDecimal purchaseCost,
        @Size(max = 120) String vendor,
        @Size(max = 120) String location,
        InventoryCondition condition
) {
}
