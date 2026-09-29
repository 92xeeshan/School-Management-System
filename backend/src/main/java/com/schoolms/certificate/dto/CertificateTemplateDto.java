package com.schoolms.certificate.dto;

import com.schoolms.certificate.CertificateType;

import java.util.UUID;

public record CertificateTemplateDto(
        UUID id,
        CertificateType type,
        String headerHtml,
        String footerHtml,
        String signatureImageUrl,
        String sealImageUrl,
        boolean active,
        boolean requiresApproval
) {
}
