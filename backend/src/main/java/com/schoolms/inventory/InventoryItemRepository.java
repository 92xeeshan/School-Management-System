package com.schoolms.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findBySchoolIdOrderByNameAsc(UUID schoolId);

    Optional<InventoryItem> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndSku(UUID schoolId, String sku);

    @Query("""
            SELECT COUNT(i) FROM InventoryItem i
            WHERE i.schoolId = :schoolId
              AND i.quantity <= i.reorderThreshold
              AND i.condition <> com.schoolms.common.enums.InventoryCondition.RETIRED
            """)
    long countLowStock(@Param("schoolId") UUID schoolId);

    @Query("""
            SELECT i FROM InventoryItem i
            WHERE i.schoolId = :schoolId
              AND i.quantity <= i.reorderThreshold
              AND i.condition <> com.schoolms.common.enums.InventoryCondition.RETIRED
            ORDER BY i.name ASC
            """)
    List<InventoryItem> findLowStock(@Param("schoolId") UUID schoolId);
}
