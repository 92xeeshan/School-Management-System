package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GradeBoundaryRepository extends JpaRepository<GradeBoundary, UUID> {

    List<GradeBoundary> findBySchemeIdAndSchoolIdOrderBySortOrderAsc(UUID schemeId, UUID schoolId);

    List<GradeBoundary> findBySchoolId(UUID schoolId);

    void deleteBySchemeIdAndSchoolId(UUID schemeId, UUID schoolId);
}
