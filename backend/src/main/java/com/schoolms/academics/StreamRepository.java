package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StreamRepository extends JpaRepository<Stream, UUID> {

    List<Stream> findBySchoolIdOrderBySortOrderAsc(UUID schoolId);

    Optional<Stream> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndName(UUID schoolId, String name);

    boolean existsBySchoolIdAndNameAndIdNot(UUID schoolId, String name, UUID id);
}
