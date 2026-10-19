package com.schoolms.academics.dto;

import com.schoolms.common.enums.AcademicTermStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AcademicTermRequest(
        @NotBlank(message = "{validation.not_blank}") String name,
        @NotNull(message = "{validation.not_null}") LocalDate startDate,
        @NotNull(message = "{validation.not_null}") LocalDate endDate,
        AcademicTermStatus status,
        Integer sortOrder
) {
}
