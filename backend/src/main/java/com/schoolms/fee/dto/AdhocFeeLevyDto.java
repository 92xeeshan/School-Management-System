package com.schoolms.fee.dto;

import com.schoolms.fee.AdhocFeeLevy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AdhocFeeLevyDto(
        UUID id,
        UUID academicYearId,
        String academicYearName,
        UUID feeCategoryId,
        String feeCategoryName,
        BigDecimal amount,
        LocalDate dueDate,
        String scope,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        UUID studentId,
        String studentName,
        String remarks,
        int assignedCount
) {
    public static AdhocFeeLevyDto from(
            AdhocFeeLevy levy,
            String yearName,
            String categoryName,
            String className,
            String sectionName,
            String studentName) {
        return new AdhocFeeLevyDto(
                levy.getId(),
                levy.getAcademicYearId(),
                yearName,
                levy.getFeeCategoryId(),
                categoryName,
                levy.getAmount(),
                levy.getDueDate(),
                levy.getScope(),
                levy.getClassId(),
                className,
                levy.getSectionId(),
                sectionName,
                levy.getStudentId(),
                studentName,
                levy.getRemarks(),
                levy.getAssignedCount());
    }
}
