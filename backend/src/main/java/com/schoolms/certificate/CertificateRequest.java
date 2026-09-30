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
import java.util.UUID;

@Entity
@Table(name = "certificate_request")
@Getter
@Setter
public class CertificateRequest extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false, length = 20)
    private CertificateType certificateType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CertificateRequestStatus status = CertificateRequestStatus.SUBMITTED;

    @Column(length = 500)
    private String reason;

    @Column(name = "conduct_remarks", length = 500)
    private String conductRemarks;

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

    @Column(name = "teacher_notes", length = 500)
    private String teacherNotes;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "supporting_doc_key", length = 500)
    private String supportingDocKey;

    @Column(name = "supporting_doc_name")
    private String supportingDocName;

    @Column(name = "requested_by_user_id", nullable = false)
    private UUID requestedByUserId;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "issued_id")
    private UUID issuedId;
}
