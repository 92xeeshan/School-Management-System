package com.schoolms.event.dto;

import java.time.Instant;
import java.util.UUID;

public record EventDto(
        UUID id,
        String title,
        String description,
        String type,
        Instant startDateTime,
        Instant endDateTime,
        boolean allDay,
        String audienceScope,
        UUID audienceRefId,
        String source,
        String syncStatus,
        String externalProvider,
        String externalRefId,
        UUID createdBy
) {
}
