package com.schoolms.academics.dto;

import com.schoolms.academics.Subject;
import com.schoolms.common.enums.SubjectType;

import java.util.UUID;

public record SubjectDto(
        UUID id,
        String name,
        String code,
        SubjectType type,
        String description
) {
    public static SubjectDto from(Subject subject) {
        return new SubjectDto(subject.getId(), subject.getName(), subject.getCode(),
                subject.getType(), subject.getDescription());
    }
}
