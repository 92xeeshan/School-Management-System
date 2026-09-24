package com.schoolms.exam.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MarksheetSubjectDto(
        UUID subjectId,
        String subjectCode,
        String subjectName,
        BigDecimal maxMarks,
        BigDecimal passingMarks,
        BigDecimal marksObtained,
        BigDecimal percentage,
        String grade,
        BigDecimal gradePoint,
        String result
) {
}
