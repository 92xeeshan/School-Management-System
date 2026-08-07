package com.schoolms.academics.dto;

import com.schoolms.academics.SchoolClass;

import java.util.UUID;

public record ClassDto(
        UUID id,
        String name,
        String code,
        Integer sortOrder
) {
    public static ClassDto from(SchoolClass schoolClass) {
        return new ClassDto(schoolClass.getId(), schoolClass.getName(),
                schoolClass.getCode(), schoolClass.getSortOrder());
    }
}
