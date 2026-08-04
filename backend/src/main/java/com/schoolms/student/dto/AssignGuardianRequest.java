package com.schoolms.student.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AssignGuardianRequest(
        @NotNull(message = "{validation.not_null}") UUID guardianId,
        @Size(max = 20) String relationship,
        boolean primary
) {
}
