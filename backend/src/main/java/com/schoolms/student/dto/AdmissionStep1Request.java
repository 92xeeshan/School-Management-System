package com.schoolms.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record AdmissionStep1Request(
        @NotBlank(message = "{validation.not_blank}") @Size(max = 60) String firstName,
        @Size(max = 60) String middleName,
        @Size(max = 60) String lastName,
        @NotNull(message = "{validation.not_null}") UUID classId,
        @NotNull(message = "{validation.not_null}") UUID sectionId,
        UUID academicYearId,
        @NotNull(message = "{validation.not_null}") @Past(message = "validation.invalid") LocalDate dateOfBirth,
        @NotBlank(message = "{validation.not_blank}") String gender,
        @Size(max = 5) String bloodGroup,
        UUID religionId,
        UUID categoryId,
        @Size(max = 60) String nationality,
        UUID motherTongueId,
        @Pattern(regexp = "^$|^\\d{12}$", message = "validation.invalid") String aadhaarNo,
        @Pattern(regexp = "^$|^\\d{12}$", message = "validation.invalid") String apaarId
) {
}
