package com.schoolms.inventory.dto;

import com.schoolms.common.enums.InventoryTransactionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record InventoryTransactionRequest(
        @NotNull InventoryTransactionType type,
        @Min(0) Integer quantity,
        @Size(max = 240) String reason,
        UUID assignedToUserId,
        @Size(max = 120) String room,
        LocalDate transactionDate
) {
}
