package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReportCardDto(
        UUID studentId,
        String studentName,
        String admissionNo,
        Integer rollNumber,
        LocalDate dateOfBirth,
        String guardianName,
        String photoUrl,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        UUID academicYearId,
        String academicYearName,
        String examTerm,
        boolean ready,
        boolean published,
        String schoolName,
        String schoolAddress,
        String schoolPhone,
        String schoolEmail,
        String affiliation,
        String classTeacherName,
        BigDecimal totalObtained,
        BigDecimal totalMax,
        BigDecimal percentage,
        String overallGrade,
        String result,
        ReportCardAttendanceDto attendance,
        List<ReportCardSubjectDto> subjects,
        ReportCardBehaviourDto behaviour,
        String teacherComment,
        String principalComment
) {
}
