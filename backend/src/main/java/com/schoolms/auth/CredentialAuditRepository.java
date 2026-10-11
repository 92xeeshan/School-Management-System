package com.schoolms.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CredentialAuditRepository extends JpaRepository<CredentialAudit, UUID> {

    List<CredentialAudit> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    List<CredentialAudit> findBySchoolIdAndTargetUserIdOrderByCreatedAtDesc(UUID schoolId, UUID targetUserId);
}
