package com.schoolms.quiz.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QuizOfflineRowDto(
        UUID studentId,
        String admissionNo,
        String studentName,
        Integer rollNumber,
        BigDecimal marks,
        String remarks
) {
}
