package com.schoolms.certificate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCertificateRequest(
        @NotBlank @Size(max = 500) String rejectionReason
) {
}
