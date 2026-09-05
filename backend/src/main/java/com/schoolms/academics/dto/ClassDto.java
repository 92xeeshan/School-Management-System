package com.schoolms.academics.dto;

import com.schoolms.academics.SchoolClass;

import java.util.List;
import java.util.UUID;

public record ClassDto(
        UUID id,
        String name,
        String code,
        Integer sortOrder,
        List<SectionDto> sections,
        List<SubjectDto> subjects
) {
    public static ClassDto from(SchoolClass schoolClass) {
        return new ClassDto(schoolClass.getId(), schoolClass.getName(),
                schoolClass.getCode(), schoolClass.getSortOrder(), List.of(), List.of());
    }
}
