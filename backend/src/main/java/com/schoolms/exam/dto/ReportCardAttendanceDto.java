package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportCardAttendanceDto(
        long workingDays,
        long daysPresent,
        long daysAbsent,
        long daysLate,
        long daysLeave,
        BigDecimal percent,
        LocalDate fromDate,
        LocalDate toDate
) {
}
