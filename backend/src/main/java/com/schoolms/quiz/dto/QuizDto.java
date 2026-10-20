package com.schoolms.quiz.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizDto(
        UUID id,
        UUID academicYearId,
        String academicYearName,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        UUID subjectId,
        String subjectName,
        String title,
        String instructions,
        int durationMinutes,
        int maxAttempts,
        Instant startAt,
        Instant endAt,
        BigDecimal passMarks,
        boolean shuffleQuestions,
        boolean showResults,
        String status,
        int questionCount,
        BigDecimal maxScore,
        int attemptCount,
        Instant createdAt,
        List<QuizItemDto> items
) {
}
