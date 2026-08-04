package com.schoolms.fee.dto;

import com.schoolms.fee.FeeStructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FeeStructureDto(
        UUID id,
        UUID classId,
        String className,
        UUID academicYearId,
        UUID categoryId,
        String categoryName,
        BigDecimal amount,
        String frequency,
        Short dueDay,
        LocalDate applicableFrom,
        LocalDate applicableTo
) {
    public static FeeStructureDto from(FeeStructure structure, String className, String categoryName) {
        return new FeeStructureDto(structure.getId(), structure.getClassId(), className,
                structure.getAcademicYearId(), structure.getCategoryId(), categoryName,
                structure.getAmount(), structure.getFrequency().name(), structure.getDueDay(),
                structure.getApplicableFrom(), structure.getApplicableTo());
    }
}
