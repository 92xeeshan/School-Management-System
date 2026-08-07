package com.schoolms.fee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, UUID> {

    List<FeeStructure> findBySchoolIdAndAcademicYearIdOrderByClassIdAsc(UUID schoolId, UUID academicYearId);

    Optional<FeeStructure> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndClassIdAndAcademicYearIdAndCategoryId(
            UUID schoolId, UUID classId, UUID academicYearId, UUID categoryId);
}
