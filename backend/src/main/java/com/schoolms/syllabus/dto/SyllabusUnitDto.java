package com.schoolms.syllabus.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SyllabusUnitDto(
        UUID id,
        String title,
        String description,
        int sortOrder,
        LocalDate targetCompletionDate,
        int totalChapters,
        int completedChapters,
        int completionPercent,
        boolean overdue,
        List<SyllabusChapterDto> chapters
) {
}
