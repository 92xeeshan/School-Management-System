package com.schoolms.academics;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "promotion_batch")
@Getter
@Setter
public class PromotionBatch extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "source_year_id", nullable = false)
    private UUID sourceYearId;

    @Column(name = "destination_year_id", nullable = false)
    private UUID destinationYearId;

    @Column(name = "source_class_id", nullable = false)
    private UUID sourceClassId;

    @Column(name = "source_section_id")
    private UUID sourceSectionId;

    @Column(name = "destination_class_id", nullable = false)
    private UUID destinationClassId;

    @Column(name = "destination_section_id", nullable = false)
    private UUID destinationSectionId;

    @Column(name = "promoted_count", nullable = false)
    private int promotedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;
}
