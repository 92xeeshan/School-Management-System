package com.schoolms.certificate.dto;

import com.schoolms.certificate.CertificateType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CertificateTemplateRequest(
        @NotNull CertificateType type,
        @Size(max = 4000) String headerHtml,
        @Size(max = 4000) String footerHtml,
        @Size(max = 500) String signatureImageUrl,
        @Size(max = 500) String sealImageUrl,
        Boolean active,
        Boolean requiresApproval
) {
}
