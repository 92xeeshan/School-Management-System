package com.schoolms.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record NoticeRequest(
        @NotBlank(message = "{validation.not_blank}") String title,
        @NotBlank(message = "{validation.not_blank}") @Size(max = 5000) String body,
        String visibilityScope,
        UUID classId,
        UUID sectionId,
        String priority,
        Instant publishAt,
        Instant expiresAt,
        String status
) {
}
