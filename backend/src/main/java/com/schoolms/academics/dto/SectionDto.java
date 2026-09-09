package com.schoolms.academics.dto;

import com.schoolms.academics.Section;

import java.util.UUID;

public record SectionDto(
        UUID id,
        UUID classId,
        String className,
        String name,
        Integer capacity,
        String room,
        Integer studentCount,
        UUID classTeacherId,
        String classTeacherName
) {
    public static SectionDto from(Section section, String className) {
        return new SectionDto(section.getId(), section.getClassId(), className,
                section.getName(), section.getCapacity(), section.getRoom(), 0, null, null);
    }
}
