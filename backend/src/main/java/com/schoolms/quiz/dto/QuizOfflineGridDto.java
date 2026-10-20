package com.schoolms.quiz.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuizOfflineGridDto(
        UUID quizId,
        String quizTitle,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        UUID subjectId,
        String subjectName,
        BigDecimal maxScore,
        boolean canEdit,
        List<QuizOfflineRowDto> rows
) {
}
