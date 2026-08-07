package com.schoolms.student.dto;

import java.util.UUID;

public record StudentGuardianDto(
        UUID id,
        GuardianDto guardian,
        String relationship,
        boolean primary
) {
}
