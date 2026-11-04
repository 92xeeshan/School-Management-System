package com.schoolms.academics;

import com.schoolms.common.enums.LookupKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolLookupRepository extends JpaRepository<SchoolLookup, UUID> {

    List<SchoolLookup> findBySchoolIdAndKindOrderBySortOrderAsc(UUID schoolId, LookupKind kind);

    Optional<SchoolLookup> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndKindAndName(UUID schoolId, LookupKind kind, String name);

    boolean existsBySchoolIdAndKindAndNameAndIdNot(UUID schoolId, LookupKind kind, String name, UUID id);
}
