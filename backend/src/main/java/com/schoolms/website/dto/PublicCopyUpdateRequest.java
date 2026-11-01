package com.schoolms.website.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PublicCopyUpdateRequest(
        @NotBlank @Pattern(regexp = "en|hi|ur") String locale,
        @Size(max = 300) String tagline,
        String about,
        String principalMessage,
        String facilities
) {
}
