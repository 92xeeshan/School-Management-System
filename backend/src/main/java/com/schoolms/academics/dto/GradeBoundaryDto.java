package com.schoolms.academics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record GradeBoundaryDto(
        UUID id,
        String label,
        BigDecimal minPercent,
        BigDecimal maxPercent,
        BigDecimal gpaValue,
        Integer sortOrder
) {
}
