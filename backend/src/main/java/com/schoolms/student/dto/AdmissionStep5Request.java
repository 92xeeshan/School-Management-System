package com.schoolms.student.dto;

import jakarta.validation.Valid;

public record AdmissionStep5Request(
        @Valid PreviousSchoolDto previousSchool
) {
}
