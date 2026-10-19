package com.schoolms.syllabus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyllabusCourseRepository extends JpaRepository<SyllabusCourse, UUID> {

    List<SyllabusCourse> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    Optional<SyllabusCourse> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<SyllabusCourse> findBySchoolIdAndAcademicYearIdAndClassIdAndSubjectId(
            UUID schoolId, UUID academicYearId, UUID classId, UUID subjectId);
}
