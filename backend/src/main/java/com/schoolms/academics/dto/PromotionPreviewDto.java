package com.schoolms.academics.dto;

import java.util.List;
import java.util.UUID;

public record PromotionPreviewDto(
        UUID sourceYearId,
        String sourceYearName,
        UUID destinationYearId,
        String destinationYearName,
        String sourceClassName,
        String sourceSectionName,
        String destinationClassName,
        String destinationSectionName,
        int eligibleCount,
        int skippedCount,
        int destinationOccupied,
        int destinationCapacity,
        List<PromotionStudentDto> students
) {
}
