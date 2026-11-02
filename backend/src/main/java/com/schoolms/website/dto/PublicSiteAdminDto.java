package com.schoolms.website.dto;

import java.util.List;
import java.util.UUID;

public record PublicSiteAdminDto(
        UUID id,
        UUID schoolId,
        boolean published,
        String heroImageUrl,
        String principalName,
        List<PublicCopyDto> copies
) {
}
