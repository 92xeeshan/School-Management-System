package com.schoolms.notice.dto;

import java.time.Instant;
import java.util.UUID;

public record NoticeDto(
        UUID id,
        String title,
        String body,
        UUID authorId,
        String authorName,
        String visibilityScope,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        String priority,
        Instant publishAt,
        Instant expiresAt,
        String status,
        boolean read,
        long readCount,
        java.util.List<AttachmentDto> attachments
) {
    public record AttachmentDto(UUID id, String fileName, String fileType, Long fileSize) {
    }
}
