package com.schoolms.student.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdmissionStep4Request(
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        LocalDate admissionDate,
        Integer rollNumber,
        UUID houseId,
        UUID mediumId,
        UUID streamId,
        List<UUID> subjectIds,
        UUID secondLanguageId,
        UUID thirdLanguageId
) {
}
