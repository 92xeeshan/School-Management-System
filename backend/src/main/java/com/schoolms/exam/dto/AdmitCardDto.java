package com.schoolms.exam.dto;

import java.util.List;
import java.util.UUID;

public record AdmitCardDto(
        UUID studentId,
        String studentName,
        String admissionNo,
        Integer rollNumber,
        String photoUrl,
        UUID classId,
        String className,
        UUID sectionId,
        String sectionName,
        UUID academicYearId,
        String academicYearName,
        String examTerm,
        boolean published,
        String schoolName,
        String schoolAddress,
        String schoolPhone,
        List<AdmitCardSlotDto> datesheet,
        List<String> guidelines
) {
}
