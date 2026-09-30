package com.schoolms.certificate.dto;

import com.schoolms.certificate.CertificateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitCertificateRequest(
        @NotNull CertificateType type,
        @NotBlank @Size(max = 500) String reason
) {
}
