package com.schoolms.fee.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CloneFeeStructureRequest(
        @NotNull(message = "{validation.not_null}") UUID sourceAcademicYearId,
        @NotNull(message = "{validation.not_null}") UUID targetAcademicYearId
) {
}
