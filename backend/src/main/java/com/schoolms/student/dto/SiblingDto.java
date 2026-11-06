package com.schoolms.student.dto;

import java.util.UUID;

public record SiblingDto(
        UUID id,
        String admissionNo,
        String displayName,
        String className,
        String sectionName
) {
}
