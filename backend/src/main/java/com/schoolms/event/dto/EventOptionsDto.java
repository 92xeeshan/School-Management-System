package com.schoolms.event.dto;

import java.util.List;
import java.util.UUID;

public record EventOptionsDto(
        List<String> eventTypes,
        List<String> visibilityScopes,
        List<String> roles,
        List<ClassOption> classes
) {
    public record ClassOption(UUID id, String name, List<SectionOption> sections) {
    }

    public record SectionOption(UUID id, String name) {
    }
}
