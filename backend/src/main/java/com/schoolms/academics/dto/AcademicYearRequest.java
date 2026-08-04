package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AcademicYearRequest(
        @NotBlank(message = "{validation.not_blank}") String name,
        @NotNull(message = "{validation.not_null}") LocalDate startDate,
        @NotNull(message = "{validation.not_null}") LocalDate endDate,
        boolean current
) {
}
