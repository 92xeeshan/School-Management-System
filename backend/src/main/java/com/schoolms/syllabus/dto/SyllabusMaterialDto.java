package com.schoolms.syllabus.dto;

import java.time.Instant;
import java.util.UUID;

public record SyllabusMaterialDto(
        UUID id,
        UUID unitId,
        String kind,
        String title,
        String fileName,
        String contentType,
        Long fileSize,
        String externalUrl,
        boolean downloadable,
        Instant createdAt
) {
}
