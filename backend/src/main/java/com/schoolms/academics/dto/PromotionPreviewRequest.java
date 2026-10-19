package com.schoolms.academics.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PromotionPreviewRequest(
        @NotNull(message = "{validation.not_null}") UUID sourceYearId,
        @NotNull(message = "{validation.not_null}") UUID sourceClassId,
        UUID sourceSectionId,
        @NotNull(message = "{validation.not_null}") UUID destinationYearId,
        @NotNull(message = "{validation.not_null}") UUID destinationClassId,
        @NotNull(message = "{validation.not_null}") UUID destinationSectionId,
        List<UUID> studentIds,
        String notes
) {
}
