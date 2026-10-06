package com.schoolms.payroll.dto;

import com.schoolms.common.enums.PayrollRunStatus;
import com.schoolms.payroll.PayrollRun;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayrollRunDto(
        UUID id,
        short year,
        short month,
        PayrollRunStatus status,
        Instant processedAt,
        Instant publishedAt,
        Instant paidAt,
        String notes,
        int payslipCount,
        BigDecimal netTotal
) {
    public static PayrollRunDto from(PayrollRun run, int payslipCount, BigDecimal netTotal) {
        return new PayrollRunDto(
                run.getId(),
                run.getYear(),
                run.getMonth(),
                run.getStatus(),
                run.getProcessedAt(),
                run.getPublishedAt(),
                run.getPaidAt(),
                run.getNotes(),
                payslipCount,
                netTotal);
    }
}
