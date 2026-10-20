package com.schoolms.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizRequest(
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        @NotNull(message = "{validation.not_null}") UUID classId,
        UUID sectionId,
        @NotNull(message = "{validation.not_null}") UUID subjectId,
        @NotBlank(message = "{validation.not_blank}") String title,
        String instructions,
        @NotNull(message = "{validation.not_null}") Integer durationMinutes,
        Integer maxAttempts,
        Instant startAt,
        Instant endAt,
        BigDecimal passMarks,
        boolean shuffleQuestions,
        Boolean showResults,
        @Valid List<QuizItemRequest> items
) {
}
