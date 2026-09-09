package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    List<SchoolClass> findBySchoolIdOrderBySortOrderAsc(UUID schoolId);

    Optional<SchoolClass> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<SchoolClass> findBySchoolIdAndName(UUID schoolId, String name);

    boolean existsBySchoolIdAndName(UUID schoolId, String name);

    boolean existsBySchoolIdAndNameAndIdNot(UUID schoolId, String name, UUID id);
}
