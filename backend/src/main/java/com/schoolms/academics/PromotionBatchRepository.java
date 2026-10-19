package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionBatchRepository extends JpaRepository<PromotionBatch, UUID> {

    List<PromotionBatch> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    Optional<PromotionBatch> findByIdAndSchoolId(UUID id, UUID schoolId);
}
