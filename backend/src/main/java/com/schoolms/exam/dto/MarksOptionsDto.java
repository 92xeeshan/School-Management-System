package com.schoolms.exam.dto;

import java.util.List;
import java.util.UUID;

public record MarksOptionsDto(
        List<YearOption> academicYears,
        List<ClassOption> classes,
        List<String> examTerms
) {
    public record YearOption(UUID id, String name, boolean current) {
    }

    public record ClassOption(UUID id, String name, List<SectionOption> sections, List<SubjectOption> subjects) {
    }

    public record SectionOption(UUID id, String name) {
    }

    public record SubjectOption(UUID id, String name, boolean practical) {
    }
}
