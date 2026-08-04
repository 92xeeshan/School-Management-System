package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SectionRepository extends JpaRepository<Section, UUID> {

    List<Section> findBySchoolIdAndClassIdOrderByNameAsc(UUID schoolId, UUID classId);

    List<Section> findBySchoolIdOrderByNameAsc(UUID schoolId);

    Optional<Section> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsByClassIdAndName(UUID classId, String name);
}
