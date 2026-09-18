package com.schoolms.exam.dto;

import java.util.List;
import java.util.UUID;

public record ExamScheduleOptionsDto(
        List<YearOption> academicYears,
        List<ClassOption> classes,
        List<SubjectOption> subjects,
        List<TeacherOption> teachers,
        List<String> rooms,
        List<String> examTerms
) {
    public record YearOption(UUID id, String name, boolean current) {
    }

    public record ClassOption(UUID id, String name, List<SectionOption> sections) {
    }

    public record SectionOption(UUID id, String name, String room) {
    }

    public record SubjectOption(UUID id, String name) {
    }

    public record TeacherOption(UUID id, String displayName) {
    }
}
