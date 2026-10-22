package com.schoolms.exam.dto;

import java.math.BigDecimal;

public record StudentTrendPointDto(
        String examTerm,
        boolean published,
        BigDecimal percentage,
        String overallGrade,
        String result
) {
}
