package com.schoolms.academics.dto;

import java.util.List;

public record AcademicYearDetailDto(
        AcademicYearDto year,
        List<AcademicTermDto> terms
) {
}
