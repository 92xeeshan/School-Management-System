package com.schoolms.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        String title,
        String message,
        String category,
        String actionUrl,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}
