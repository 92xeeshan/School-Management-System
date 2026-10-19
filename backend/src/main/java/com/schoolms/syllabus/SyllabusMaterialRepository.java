package com.schoolms.syllabus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyllabusMaterialRepository extends JpaRepository<SyllabusMaterial, UUID> {

    List<SyllabusMaterial> findBySchoolIdAndCourseIdOrderByCreatedAtDesc(UUID schoolId, UUID courseId);

    Optional<SyllabusMaterial> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndCourseIdAndUnitId(UUID schoolId, UUID courseId, UUID unitId);
}
