package com.schoolms.staff.dto;

import java.util.UUID;

/**
 * Lightweight section reference used by the class teacher assignment picker.
 */
public record SectionRefDto(
        UUID id,
        UUID classId,
        String className,
        String name,
        String label,
        UUID classTeacherId,
        String classTeacherName
) {
}
