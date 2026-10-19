package com.schoolms.syllabus.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SyllabusChapterDto(
        UUID id,
        UUID unitId,
        String title,
        String description,
        int sortOrder,
        String status,
        LocalDate targetCompletionDate,
        Instant completedAt,
        boolean overdue
) {
}
