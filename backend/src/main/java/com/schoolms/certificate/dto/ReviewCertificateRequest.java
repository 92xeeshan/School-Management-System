package com.schoolms.certificate.dto;

import jakarta.validation.constraints.Size;

public record ReviewCertificateRequest(
        @Size(max = 500) String conductRemarks,
        @Size(max = 500) String academicProgress,
        @Size(max = 200) String lastExamAttended,
        @Size(max = 500) String reason,
        @Size(max = 500) String teacherNotes,
        Boolean duesLibrary,
        Boolean duesAccounts,
        Boolean duesSports
) {
}
