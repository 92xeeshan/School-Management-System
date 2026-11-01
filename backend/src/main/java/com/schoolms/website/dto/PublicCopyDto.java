package com.schoolms.website.dto;

public record PublicCopyDto(
        String locale,
        String tagline,
        String about,
        String principalMessage,
        String facilities
) {
}
