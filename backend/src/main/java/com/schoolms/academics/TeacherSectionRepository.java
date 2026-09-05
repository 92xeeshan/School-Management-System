package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherSectionRepository extends JpaRepository<TeacherSection, UUID> {

    List<TeacherSection> findByTeacherIdAndSchoolId(UUID teacherId, UUID schoolId);

    List<TeacherSection> findBySectionIdAndSchoolId(UUID sectionId, UUID schoolId);

    List<TeacherSection> findBySchoolId(UUID schoolId);

    Optional<TeacherSection> findByTeacherIdAndSectionIdAndAcademicYearId(UUID teacherId, UUID sectionId, UUID academicYearId);

    boolean existsByTeacherIdAndSectionIdAndAcademicYearId(UUID teacherId, UUID sectionId, UUID academicYearId);

    void deleteByTeacherIdAndSchoolId(UUID teacherId, UUID schoolId);
}
