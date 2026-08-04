package com.schoolms.academics.dto;

import com.schoolms.academics.AcademicYear;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicYearDto(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean current
) {
    public static AcademicYearDto from(AcademicYear year) {
        return new AcademicYearDto(year.getId(), year.getName(), year.getStartDate(),
                year.getEndDate(), year.isCurrent());
    }
}
