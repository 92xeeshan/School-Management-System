package com.schoolms.certificate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, UUID> {

    List<CertificateTemplate> findBySchoolIdOrderByTypeAsc(UUID schoolId);

    Optional<CertificateTemplate> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<CertificateTemplate> findBySchoolIdAndType(UUID schoolId, CertificateType type);
}
