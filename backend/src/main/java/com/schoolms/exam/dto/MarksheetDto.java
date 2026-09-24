package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MarksheetDto(
        UUID studentId,
        String studentName,
        String admissionNo,
        Integer rollNumber,
        LocalDate dateOfBirth,
        String gender,
        String photoUrl,
        boolean photoPlaceholder,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        UUID academicYearId,
        String academicYearName,
        String examTerm,
        boolean ready,
        boolean published,
        boolean locked,
        String serialNo,
        LocalDate issueDate,
        String schoolName,
        String schoolAddress,
        String schoolPhone,
        String schoolEmail,
        String affiliation,
        BigDecimal totalObtained,
        BigDecimal totalMax,
        BigDecimal percentage,
        BigDecimal gpa,
        String overallGrade,
        String result,
        List<MarksheetSubjectDto> subjects
) {
}
