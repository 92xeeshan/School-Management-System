package com.schoolms.academics.dto;

import com.schoolms.academics.PromotionBatch;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PromotionBatchDto(
        UUID id,
        UUID sourceYearId,
        String sourceYearName,
        UUID destinationYearId,
        String destinationYearName,
        String sourceClassName,
        String sourceSectionName,
        String destinationClassName,
        String destinationSectionName,
        int promotedCount,
        int skippedCount,
        String notes,
        Instant createdAt,
        List<PromotionStudentDto> students
) {
    public static PromotionBatchDto from(PromotionBatch batch,
                                         String sourceYearName,
                                         String destinationYearName,
                                         String sourceClassName,
                                         String sourceSectionName,
                                         String destinationClassName,
                                         String destinationSectionName,
                                         List<PromotionStudentDto> students) {
        return new PromotionBatchDto(
                batch.getId(),
                batch.getSourceYearId(),
                sourceYearName,
                batch.getDestinationYearId(),
                destinationYearName,
                sourceClassName,
                sourceSectionName,
                destinationClassName,
                destinationSectionName,
                batch.getPromotedCount(),
                batch.getSkippedCount(),
                batch.getNotes(),
                batch.getCreatedAt(),
                students);
    }
}
