package com.schoolms.website.dto;

import java.util.List;
import java.util.UUID;

public record PublicSiteDto(
        UUID schoolId,
        String code,
        String name,
        String logoUrl,
        boolean published,
        String heroImageUrl,
        String principalName,
        PublicCopyDto copy,
        PublicContactDto contact,
        List<PublicNoticeDto> notices,
        List<PublicEventDto> events,
        List<PublicClassDto> classes
) {
}
