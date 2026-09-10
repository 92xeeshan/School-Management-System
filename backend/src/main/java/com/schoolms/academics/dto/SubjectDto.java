package com.schoolms.academics.dto;

import com.schoolms.academics.Subject;
import com.schoolms.common.enums.SubjectType;

import java.util.List;
import java.util.UUID;

public record SubjectDto(
        UUID id,
        String name,
        String code,
        SubjectType type,
        String description,
        Integer weeklyPeriods,
        boolean practical,
        String status,
        UUID teacherId,
        String teacherName,
        List<SubjectClassDto> classes
) {
    public static SubjectDto from(Subject subject) {
        return new SubjectDto(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                subject.getType(),
                subject.getDescription(),
                subject.getWeeklyPeriods(),
                subject.isPractical(),
                subject.getStatus(),
                null,
                null,
                List.of());
    }
}
