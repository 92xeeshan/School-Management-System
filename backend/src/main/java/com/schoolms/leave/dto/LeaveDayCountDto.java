package com.schoolms.leave.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveDayCountDto(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal days
) {
}
