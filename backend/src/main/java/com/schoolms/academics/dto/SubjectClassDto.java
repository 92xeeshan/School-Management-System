package com.schoolms.academics.dto;

import java.util.UUID;

public record SubjectClassDto(
        UUID id,
        String name,
        String code,
        UUID teacherId,
        String teacherName
) {
}
