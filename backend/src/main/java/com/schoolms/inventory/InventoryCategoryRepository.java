package com.schoolms.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, UUID> {

    List<InventoryCategory> findBySchoolIdOrderByNameAsc(UUID schoolId);

    Optional<InventoryCategory> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndNameIgnoreCase(UUID schoolId, String name);
}
