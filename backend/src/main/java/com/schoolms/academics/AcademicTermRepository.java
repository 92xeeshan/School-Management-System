package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, UUID> {

    List<AcademicTerm> findBySchoolIdAndAcademicYearIdOrderBySortOrderAscStartDateAsc(
            UUID schoolId, UUID academicYearId);

    Optional<AcademicTerm> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndAcademicYearIdAndName(UUID schoolId, UUID academicYearId, String name);

    boolean existsBySchoolIdAndAcademicYearIdAndNameAndIdNot(
            UUID schoolId, UUID academicYearId, String name, UUID id);

    long countByAcademicYearId(UUID academicYearId);
}
