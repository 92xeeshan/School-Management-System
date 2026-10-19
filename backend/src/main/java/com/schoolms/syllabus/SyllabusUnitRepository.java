package com.schoolms.syllabus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyllabusUnitRepository extends JpaRepository<SyllabusUnit, UUID> {

    List<SyllabusUnit> findBySchoolIdAndCourseIdOrderBySortOrderAscCreatedAtAsc(UUID schoolId, UUID courseId);

    List<SyllabusUnit> findBySchoolIdAndCourseIdInOrderBySortOrderAscCreatedAtAsc(UUID schoolId, List<UUID> courseIds);

    Optional<SyllabusUnit> findByIdAndSchoolId(UUID id, UUID schoolId);

    long countBySchoolIdAndCourseId(UUID schoolId, UUID courseId);
}
