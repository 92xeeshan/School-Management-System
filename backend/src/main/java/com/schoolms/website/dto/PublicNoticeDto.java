package com.schoolms.website.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicNoticeDto(
        UUID id,
        String title,
        String body,
        String priority,
        Instant publishAt
) {
}
