package com.schoolms.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MarksSaveRequest(
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        @NotNull(message = "{validation.not_null}") UUID classId,
        @NotNull(message = "{validation.not_null}") UUID sectionId,
        @NotNull(message = "{validation.not_null}") UUID subjectId,
        @NotBlank(message = "{validation.not_blank}") String examTerm,
        boolean submit,
        @NotNull(message = "{validation.not_null}") @Valid List<MarkRow> rows
) {
    public record MarkRow(
            @NotNull(message = "{validation.not_null}") UUID studentId,
            BigDecimal theory,
            BigDecimal practical,
            BigDecimal assignment,
            String attendanceStatus,
            String remarks
    ) {
    }
}
