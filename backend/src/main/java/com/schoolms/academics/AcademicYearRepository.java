package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    List<AcademicYear> findBySchoolIdOrderByStartDateDesc(UUID schoolId);

    Optional<AcademicYear> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<AcademicYear> findBySchoolIdAndCurrentTrue(UUID schoolId);

    boolean existsBySchoolIdAndName(UUID schoolId, String name);
}
