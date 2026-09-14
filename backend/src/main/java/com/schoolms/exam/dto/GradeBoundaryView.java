package com.schoolms.exam.dto;

import java.math.BigDecimal;

public record GradeBoundaryView(
        String label,
        BigDecimal minPercent,
        BigDecimal maxPercent
) {
}
