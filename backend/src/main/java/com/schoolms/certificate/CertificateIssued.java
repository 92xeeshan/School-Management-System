package com.schoolms.certificate;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "certificate_issued")
@Getter
@Setter
public class CertificateIssued extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false, length = 20)
    private CertificateType certificateType;

    @Column(name = "certificate_no", length = 80)
    private String certificateNo;

    @Column(name = "sequence_year")
    private Integer sequenceYear;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "issued_by_user_id", nullable = false)
    private UUID issuedByUserId;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificateStatus status = CertificateStatus.DRAFT;

    @Column(length = 500)
    private String reason;

    @Column(name = "conduct_remarks", length = 500)
    private String conductRemarks;

    @Column(name = "data_json")
    private String dataJson;

    @Column(name = "pdf_object_key", length = 500)
    private String pdfObjectKey;

    @Column(name = "is_duplicate", nullable = false)
    private boolean duplicate;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "academic_progress", length = 500)
    private String academicProgress;

    @Column(name = "last_exam_attended", length = 200)
    private String lastExamAttended;

    @Column(name = "dues_library")
    private Boolean duesLibrary;

    @Column(name = "dues_accounts")
    private Boolean duesAccounts;

    @Column(name = "dues_sports")
    private Boolean duesSports;

    @Column(name = "tc_issued_at")
    private Instant tcIssuedAt;

    @Column(name = "deactivation_scheduled_at")
    private LocalDate deactivationScheduledAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;
}
