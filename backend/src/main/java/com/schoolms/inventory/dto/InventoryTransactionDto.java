package com.schoolms.inventory.dto;

import com.schoolms.common.enums.InventoryTransactionType;
import com.schoolms.inventory.InventoryTransaction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryTransactionDto(
        UUID id,
        UUID itemId,
        InventoryTransactionType type,
        int quantity,
        String reason,
        UUID assignedToUserId,
        String assignedToName,
        String room,
        LocalDate transactionDate,
        Instant createdAt
) {
    public static InventoryTransactionDto from(InventoryTransaction txn, String assignedToName) {
        return new InventoryTransactionDto(
                txn.getId(),
                txn.getItemId(),
                txn.getType(),
                txn.getQuantity(),
                txn.getReason(),
                txn.getAssignedToUserId(),
                assignedToName,
                txn.getRoom(),
                txn.getTransactionDate(),
                txn.getCreatedAt());
    }
}
