package com.schoolms.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record CredentialAuditDto(
        UUID id,
        UUID actorUserId,
        UUID targetUserId,
        String action,
        String detail,
        Instant createdAt
) {
}
