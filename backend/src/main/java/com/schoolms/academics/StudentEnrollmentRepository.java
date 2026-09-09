package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {

    Optional<StudentEnrollment> findByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);

    List<StudentEnrollment> findBySectionIdAndAcademicYearId(UUID sectionId, UUID academicYearId);

    List<StudentEnrollment> findByStudentId(UUID studentId);

    List<StudentEnrollment> findBySchoolIdAndAcademicYearIdAndStatus(
            UUID schoolId, UUID academicYearId, String status);

    long countBySectionIdAndAcademicYearIdAndStatus(UUID sectionId, UUID academicYearId, String status);

    boolean existsByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);
}
