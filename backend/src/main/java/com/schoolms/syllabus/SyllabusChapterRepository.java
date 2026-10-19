package com.schoolms.syllabus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyllabusChapterRepository extends JpaRepository<SyllabusChapter, UUID> {

    List<SyllabusChapter> findBySchoolIdAndUnitIdInOrderBySortOrderAscCreatedAtAsc(UUID schoolId, List<UUID> unitIds);

    List<SyllabusChapter> findBySchoolIdAndUnitIdOrderBySortOrderAscCreatedAtAsc(UUID schoolId, UUID unitId);

    Optional<SyllabusChapter> findByIdAndSchoolId(UUID id, UUID schoolId);
}
