package com.schoolms.certificate;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "certificate_template")
@Getter
@Setter
public class CertificateTemplate extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificateType type;

    @Column(name = "header_html")
    private String headerHtml;

    @Column(name = "footer_html")
    private String footerHtml;

    @Column(name = "signature_image_url", length = 500)
    private String signatureImageUrl;

    @Column(name = "seal_image_url", length = 500)
    private String sealImageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval = true;
}
