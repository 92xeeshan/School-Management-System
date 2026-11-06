package com.schoolms.academics.dto;

import java.util.UUID;

public record ClassSubjectOptionDto(
        UUID subjectId,
        String name,
        String code,
        boolean compulsory,
        UUID streamId
) {
}
