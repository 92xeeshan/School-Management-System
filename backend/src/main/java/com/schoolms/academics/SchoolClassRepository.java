package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    List<SchoolClass> findBySchoolIdOrderBySortOrderAsc(UUID schoolId);

    Optional<SchoolClass> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndName(UUID schoolId, String name);
}
