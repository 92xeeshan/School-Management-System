package com.schoolms.certificate.dto;

import com.schoolms.certificate.CertificateStatus;
import com.schoolms.certificate.CertificateType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CertificateIssuedDto(
        UUID id,
        UUID studentId,
        String studentName,
        String admissionNo,
        String className,
        String sectionName,
        CertificateType certificateType,
        String certificateNo,
        LocalDate issuedDate,
        UUID issuedByUserId,
        String issuedByName,
        UUID approvedByUserId,
        String approvedByName,
        Instant approvedAt,
        CertificateStatus status,
        String reason,
        String conductRemarks,
        String guardianName,
        String academicYearName,
        String dateOfBirth,
        boolean duplicate,
        boolean canApprove,
        boolean canDownload
) {
}
