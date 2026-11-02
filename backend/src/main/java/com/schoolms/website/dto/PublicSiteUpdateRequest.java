package com.schoolms.website.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PublicSiteUpdateRequest(
        Boolean published,
        @Size(max = 500) String heroImageUrl,
        @Size(max = 120) String principalName,
        @Valid List<PublicCopyUpdateRequest> copies
) {
}
