package com.schoolms.auth.dto;

import java.util.UUID;

public record LinkedChildDto(
        UUID studentId,
        String name,
        String admissionNo,
        String className,
        String sectionName
) {
}
