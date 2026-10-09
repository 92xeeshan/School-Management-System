package com.schoolms.fee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeeStructureAuditRepository extends JpaRepository<FeeStructureAudit, UUID> {

    List<FeeStructureAudit> findBySchoolIdOrderByChangedAtDesc(UUID schoolId);

    List<FeeStructureAudit> findBySchoolIdAndAcademicYearIdOrderByChangedAtDesc(UUID schoolId, UUID academicYearId);
}
