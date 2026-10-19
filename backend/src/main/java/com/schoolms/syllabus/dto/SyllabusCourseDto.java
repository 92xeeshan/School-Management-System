package com.schoolms.syllabus.dto;

import java.time.LocalDate;
import java.util.UUID;

public record SyllabusCourseDto(
        UUID id,
        UUID academicYearId,
        String academicYearName,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        UUID subjectId,
        String subjectName,
        UUID teacherId,
        String teacherName,
        String title,
        String description,
        LocalDate targetCompletionDate,
        String status,
        String progressStatus,
        int totalUnits,
        int completedUnits,
        int totalChapters,
        int completedChapters,
        int inProgressChapters,
        int remainingChapters,
        int completionPercent,
        boolean overdue
) {
}
