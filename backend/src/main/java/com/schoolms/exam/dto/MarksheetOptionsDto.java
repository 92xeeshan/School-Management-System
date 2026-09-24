package com.schoolms.exam.dto;

import java.util.List;
import java.util.UUID;

public record MarksheetOptionsDto(
        List<YearOption> academicYears,
        List<ClassOption> classes,
        List<String> examTerms,
        boolean studentView,
        boolean canManage,
        UUID defaultStudentId,
        UUID defaultClassId,
        UUID defaultSectionId
) {
    public record YearOption(UUID id, String name, boolean current) {
    }

    public record ClassOption(UUID id, String name, List<SectionOption> sections) {
    }

    public record SectionOption(UUID id, String name) {
    }
}
