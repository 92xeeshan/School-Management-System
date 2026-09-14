package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MarksGridDto(
        UUID examEntryId,
        UUID academicYearId,
        String academicYearName,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        UUID subjectId,
        String subjectName,
        boolean practicalEnabled,
        String examTerm,
        BigDecimal maxTheory,
        BigDecimal maxPractical,
        BigDecimal maxAssignment,
        BigDecimal maxTotal,
        BigDecimal passMarks,
        String scaleType,
        String schemeName,
        LocalDate entryDeadline,
        boolean locked,
        boolean canEdit,
        boolean canLock,
        List<GradeBoundaryView> boundaries,
        List<ExamMarkRowDto> rows
) {
}
