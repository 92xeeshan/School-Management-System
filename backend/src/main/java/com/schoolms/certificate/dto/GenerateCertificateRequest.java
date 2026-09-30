package com.schoolms.certificate.dto;

import com.schoolms.certificate.CertificateType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GenerateCertificateRequest(
        @NotNull UUID studentId,
        @NotNull CertificateType templateType,
        @Size(max = 500) String reason,
        @Size(max = 500) String conductRemarks,
        @Size(max = 500) String academicProgress,
        @Size(max = 200) String lastExamAttended,
        Boolean duesLibrary,
        Boolean duesAccounts,
        Boolean duesSports,
        Boolean duplicate
) {
    public GenerateCertificateRequest(UUID studentId, CertificateType templateType, String reason, String conductRemarks) {
        this(studentId, templateType, reason, conductRemarks, null, null, null, null, null, false);
    }
}
