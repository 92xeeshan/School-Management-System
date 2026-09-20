package com.schoolms.exam.dto;

import java.util.UUID;

public record AdmitCardStudentDto(
        UUID studentId,
        String studentName,
        String admissionNo,
        Integer rollNumber,
        String photoUrl,
        boolean published,
        int examCount
) {
}
