package com.schoolms.academics.dto;

import com.schoolms.academics.AcademicTerm;
import com.schoolms.common.enums.AcademicTermStatus;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicTermDto(
        UUID id,
        UUID academicYearId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        AcademicTermStatus status,
        Integer sortOrder
) {
    public static AcademicTermDto from(AcademicTerm term) {
        return new AcademicTermDto(term.getId(), term.getAcademicYearId(), term.getName(),
                term.getStartDate(), term.getEndDate(), term.getStatus(), term.getSortOrder());
    }
}
