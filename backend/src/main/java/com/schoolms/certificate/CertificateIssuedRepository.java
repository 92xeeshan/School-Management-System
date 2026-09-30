package com.schoolms.certificate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateIssuedRepository extends JpaRepository<CertificateIssued, UUID> {

    Optional<CertificateIssued> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<CertificateIssued> findBySchoolIdAndStudentIdOrderByCreatedAtDesc(UUID schoolId, UUID studentId);

    Page<CertificateIssued> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    Page<CertificateIssued> findBySchoolIdAndCertificateTypeOrderByCreatedAtDesc(
            UUID schoolId, CertificateType type, Pageable pageable);

    Page<CertificateIssued> findBySchoolIdAndStatusOrderByCreatedAtDesc(
            UUID schoolId, CertificateStatus status, Pageable pageable);

    Page<CertificateIssued> findBySchoolIdAndCertificateTypeAndStatusOrderByCreatedAtDesc(
            UUID schoolId, CertificateType type, CertificateStatus status, Pageable pageable);

    Page<CertificateIssued> findBySchoolIdAndStudentIdInOrderByCreatedAtDesc(
            UUID schoolId, Collection<UUID> studentIds, Pageable pageable);

    Page<CertificateIssued> findBySchoolIdAndStudentIdInAndCertificateTypeOrderByCreatedAtDesc(
            UUID schoolId, Collection<UUID> studentIds, CertificateType type, Pageable pageable);

    Page<CertificateIssued> findBySchoolIdAndStudentIdInAndStatusOrderByCreatedAtDesc(
            UUID schoolId, Collection<UUID> studentIds, CertificateStatus status, Pageable pageable);

    Page<CertificateIssued> findBySchoolIdAndStudentIdInAndCertificateTypeAndStatusOrderByCreatedAtDesc(
            UUID schoolId, Collection<UUID> studentIds, CertificateType type, CertificateStatus status, Pageable pageable);

    @Query("""
            select count(c) from CertificateIssued c
            where c.schoolId = :schoolId
              and c.certificateType = :type
              and c.sequenceYear = :year
              and c.certificateNo is not null
            """)
    long countNumbered(@Param("schoolId") UUID schoolId,
                       @Param("type") CertificateType type,
                       @Param("year") int year);

    @Query("""
            select c from CertificateIssued c
            where c.certificateType = com.schoolms.certificate.CertificateType.TC
              and c.status = com.schoolms.certificate.CertificateStatus.ISSUED
              and c.deactivationScheduledAt is not null
              and c.deactivationScheduledAt <= :today
              and c.deactivatedAt is null
            """)
    List<CertificateIssued> findDueForDeactivation(@Param("today") java.time.LocalDate today);
}
