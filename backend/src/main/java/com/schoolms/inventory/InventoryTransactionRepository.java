package com.schoolms.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    List<InventoryTransaction> findBySchoolIdAndItemIdOrderByTransactionDateDescCreatedAtDesc(
            UUID schoolId, UUID itemId);
}
