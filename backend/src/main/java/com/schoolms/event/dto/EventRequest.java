package com.schoolms.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EventRequest(
        @NotBlank(message = "{validation.not_blank}") String title,
        String description,
        @NotBlank(message = "{validation.not_blank}") String type,
        @NotNull(message = "{validation.not_null}") Instant startDateTime,
        Instant endDateTime,
        boolean allDay,
        String audienceScope,
        UUID audienceRefId,
        String source,
        String syncStatus,
        String externalProvider,
        String externalRefId
) {
}
