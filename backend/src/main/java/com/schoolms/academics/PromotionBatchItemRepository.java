package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromotionBatchItemRepository extends JpaRepository<PromotionBatchItem, UUID> {

    List<PromotionBatchItem> findByBatchIdOrderByCreatedAtAsc(UUID batchId);
}
