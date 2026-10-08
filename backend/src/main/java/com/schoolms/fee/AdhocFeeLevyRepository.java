package com.schoolms.fee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdhocFeeLevyRepository extends JpaRepository<AdhocFeeLevy, UUID> {

    List<AdhocFeeLevy> findBySchoolIdAndAcademicYearIdOrderByCreatedAtDesc(UUID schoolId, UUID academicYearId);

    Optional<AdhocFeeLevy> findByIdAndSchoolId(UUID id, UUID schoolId);
}
