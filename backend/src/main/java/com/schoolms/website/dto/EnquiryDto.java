package com.schoolms.website.dto;

import java.time.Instant;
import java.util.UUID;

public record EnquiryDto(
        UUID id,
        String parentName,
        String phone,
        String email,
        UUID classId,
        String className,
        String message,
        String status,
        String notes,
        Instant createdAt,
        Instant reviewedAt
) {
}
