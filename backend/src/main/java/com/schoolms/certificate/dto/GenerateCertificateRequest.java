package com.schoolms.certificate.dto;

import com.schoolms.certificate.CertificateType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GenerateCertificateRequest(
        @NotNull UUID studentId,
        @NotNull CertificateType templateType,
        @Size(max = 500) String reason,
        @Size(max = 500) String conductRemarks
) {
}
