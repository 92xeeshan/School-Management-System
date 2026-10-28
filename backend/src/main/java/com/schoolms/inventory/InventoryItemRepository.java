package com.schoolms.inventory;

import com.schoolms.common.enums.InventoryCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findBySchoolIdOrderByNameAsc(UUID schoolId);

    @Query("""
            SELECT i FROM InventoryItem i
            WHERE i.schoolId = :schoolId
              AND (:categoryId IS NULL OR i.categoryId = :categoryId)
              AND (:location IS NULL OR LOWER(i.location) = :location)
              AND (:condition IS NULL OR i.condition = :condition)
              AND (:lowStock IS NULL OR :lowStock = FALSE
                OR (i.quantity <= i.reorderThreshold
                    AND i.condition <> com.schoolms.common.enums.InventoryCondition.RETIRED))
              AND (:like IS NULL
                OR LOWER(i.name) LIKE :like
                OR LOWER(COALESCE(i.sku, '')) LIKE :like
                OR LOWER(COALESCE(i.location, '')) LIKE :like
                OR LOWER(COALESCE(i.vendor, '')) LIKE :like)
            """)
    Page<InventoryItem> searchDirectory(@Param("schoolId") UUID schoolId,
                                        @Param("categoryId") UUID categoryId,
                                        @Param("location") String location,
                                        @Param("condition") InventoryCondition condition,
                                        @Param("lowStock") Boolean lowStock,
                                        @Param("like") String like,
                                        Pageable pageable);

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
