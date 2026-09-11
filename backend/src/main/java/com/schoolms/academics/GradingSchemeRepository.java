package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradingSchemeRepository extends JpaRepository<GradingScheme, UUID> {

    List<GradingScheme> findBySchoolIdOrderByNameAsc(UUID schoolId);

    List<GradingScheme> findBySchoolIdAndAcademicYearIdOrderByNameAsc(UUID schoolId, UUID academicYearId);

    Optional<GradingScheme> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndAcademicYearIdAndClassIdAndName(UUID schoolId, UUID academicYearId, UUID classId, String name);

    boolean existsBySchoolIdAndAcademicYearIdAndClassIdAndNameAndIdNot(
            UUID schoolId, UUID academicYearId, UUID classId, String name, UUID id);

    Optional<GradingScheme> findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(
            UUID schoolId, UUID academicYearId, UUID classId, String status);
}
