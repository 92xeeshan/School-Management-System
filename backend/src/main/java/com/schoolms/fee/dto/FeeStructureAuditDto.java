package com.schoolms.fee.dto;

import com.schoolms.fee.FeeStructureAudit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FeeStructureAuditDto(
        UUID id,
        UUID feeStructureId,
        UUID academicYearId,
        String academicYearName,
        UUID classId,
        String className,
        UUID categoryId,
        String categoryName,
        String action,
        BigDecimal previousAmount,
        BigDecimal newAmount,
        String previousFrequency,
        String newFrequency,
        UUID changedBy,
        String changedByName,
        Instant changedAt
) {
    public static FeeStructureAuditDto from(
            FeeStructureAudit audit,
            String yearName,
            String className,
            String categoryName,
            String changedByName) {
        return new FeeStructureAuditDto(
                audit.getId(),
                audit.getFeeStructureId(),
                audit.getAcademicYearId(),
                yearName,
                audit.getClassId(),
                className,
                audit.getCategoryId(),
                categoryName,
                audit.getAction(),
                audit.getPreviousAmount(),
                audit.getNewAmount(),
                audit.getPreviousFrequency(),
                audit.getNewFrequency(),
                audit.getChangedBy(),
                changedByName,
                audit.getChangedAt());
    }
}
