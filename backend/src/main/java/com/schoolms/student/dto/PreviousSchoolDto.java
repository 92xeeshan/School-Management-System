package com.schoolms.student.dto;

import com.schoolms.student.StudentPreviousSchool;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PreviousSchoolDto(
        boolean noPreviousSchool,
        @Size(max = 200) String schoolName,
        @Size(max = 40) String board,
        @Size(max = 80) String medium,
        @Size(max = 50) String lastClass,
        @Size(max = 10) String yearOfPassing,
        @Size(max = 20) String percentage,
        @Size(max = 60) String tcNumber,
        LocalDate tcDate,
        @Size(max = 300) String reason
) {
    public static PreviousSchoolDto from(StudentPreviousSchool row) {
        return new PreviousSchoolDto(row.isNoPreviousSchool(), row.getSchoolName(), row.getBoard(),
                row.getMedium(), row.getLastClass(), row.getYearOfPassing(), row.getPercentage(),
                row.getTcNumber(), row.getTcDate(), row.getReason());
    }
}
