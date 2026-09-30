package com.schoolms.certificate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRequestRepository extends JpaRepository<CertificateRequest, UUID> {

    Optional<CertificateRequest> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<CertificateRequest> findBySchoolIdAndStudentIdOrderByCreatedAtDesc(UUID schoolId, UUID studentId);

    Page<CertificateRequest> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    Page<CertificateRequest> findBySchoolIdAndStatusOrderByCreatedAtDesc(
            UUID schoolId, CertificateRequestStatus status, Pageable pageable);

    Page<CertificateRequest> findBySchoolIdAndCertificateTypeOrderByCreatedAtDesc(
            UUID schoolId, CertificateType type, Pageable pageable);

    Page<CertificateRequest> findBySchoolIdAndCertificateTypeAndStatusOrderByCreatedAtDesc(
            UUID schoolId, CertificateType type, CertificateRequestStatus status, Pageable pageable);

    Page<CertificateRequest> findBySchoolIdAndStudentIdInOrderByCreatedAtDesc(
            UUID schoolId, Collection<UUID> studentIds, Pageable pageable);

    Page<CertificateRequest> findBySchoolIdAndStudentIdInAndStatusOrderByCreatedAtDesc(
            UUID schoolId, Collection<UUID> studentIds, CertificateRequestStatus status, Pageable pageable);

    Page<CertificateRequest> findBySchoolIdAndStudentIdInAndCertificateTypeOrderByCreatedAtDesc(
            UUID schoolId, Collection<UUID> studentIds, CertificateType type, Pageable pageable);

    Page<CertificateRequest> findBySchoolIdAndStudentIdInAndCertificateTypeAndStatusOrderByCreatedAtDesc(
            UUID schoolId, Collection<UUID> studentIds, CertificateType type, CertificateRequestStatus status, Pageable pageable);

    boolean existsBySchoolIdAndStudentIdAndCertificateTypeAndStatusIn(
            UUID schoolId, UUID studentId, CertificateType type, Collection<CertificateRequestStatus> statuses);
}
