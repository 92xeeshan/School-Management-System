package com.schoolms.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuizOfflineSaveRequest(
        @NotNull(message = "{validation.not_null}") @Valid List<Row> rows
) {
    public record Row(
            @NotNull(message = "{validation.not_null}") UUID studentId,
            BigDecimal marks,
            String remarks
    ) {
    }
}
