package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentWeightageRepository extends JpaRepository<AssessmentWeightage, UUID> {

    List<AssessmentWeightage> findBySchemeIdAndSchoolIdOrderByAssessmentTypeAsc(UUID schemeId, UUID schoolId);

    List<AssessmentWeightage> findBySchoolId(UUID schoolId);

    void deleteBySchemeIdAndSchoolId(UUID schemeId, UUID schoolId);
}
