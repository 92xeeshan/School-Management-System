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

    @Query("""
            SELECT r FROM CertificateRequest r
            WHERE r.schoolId = :schoolId
              AND (:type IS NULL OR r.certificateType = :type)
              AND (:status IS NULL OR r.status = :status)
              AND (:like IS NULL OR EXISTS (
                    SELECT 1 FROM Student s
                    WHERE s.id = r.studentId AND s.schoolId = :schoolId
                      AND (LOWER(s.firstName) LIKE :like
                        OR LOWER(COALESCE(s.lastName, '')) LIKE :like
                        OR LOWER(s.admissionNo) LIKE :like)))
            """)
    Page<CertificateRequest> searchDirectory(@Param("schoolId") UUID schoolId,
                                             @Param("type") CertificateType type,
                                             @Param("status") CertificateRequestStatus status,
                                             @Param("like") String like,
                                             Pageable pageable);

    @Query("""
            SELECT r FROM CertificateRequest r
            WHERE r.schoolId = :schoolId
              AND r.studentId IN :studentIds
              AND (:type IS NULL OR r.certificateType = :type)
              AND (:status IS NULL OR r.status = :status)
              AND (:like IS NULL OR EXISTS (
                    SELECT 1 FROM Student s
                    WHERE s.id = r.studentId AND s.schoolId = :schoolId
                      AND (LOWER(s.firstName) LIKE :like
                        OR LOWER(COALESCE(s.lastName, '')) LIKE :like
                        OR LOWER(s.admissionNo) LIKE :like)))
            """)
    Page<CertificateRequest> searchDirectoryScoped(@Param("schoolId") UUID schoolId,
                                                   @Param("studentIds") Collection<UUID> studentIds,
                                                   @Param("type") CertificateType type,
                                                   @Param("status") CertificateRequestStatus status,
                                                   @Param("like") String like,
                                                   Pageable pageable);
}
