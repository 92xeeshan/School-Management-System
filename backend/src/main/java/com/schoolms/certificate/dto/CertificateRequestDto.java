package com.schoolms.certificate.dto;

import com.schoolms.certificate.CertificateRequestStatus;
import com.schoolms.certificate.CertificateType;

import java.time.Instant;
import java.util.UUID;

public record CertificateRequestDto(
        UUID id,
        UUID studentId,
        String studentName,
        String admissionNo,
        String rollNo,
        String className,
        String sectionName,
        String dateOfBirth,
        String guardianName,
        CertificateType certificateType,
        CertificateRequestStatus status,
        String reason,
        String conductRemarks,
        String academicProgress,
        String lastExamAttended,
        Boolean duesLibrary,
        Boolean duesAccounts,
        Boolean duesSports,
        String teacherNotes,
        String rejectionReason,
        String supportingDocName,
        UUID issuedId,
        String certificateNo,
        Instant createdAt,
        Instant reviewedAt,
        Instant approvedAt,
        boolean canReview,
        boolean canCancel,
        boolean canApprove,
        boolean canReject,
        boolean canDownload
) {
}
