package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {

    Optional<StudentEnrollment> findByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);

    List<StudentEnrollment> findBySectionIdAndAcademicYearId(UUID sectionId, UUID academicYearId);

    List<StudentEnrollment> findByStudentId(UUID studentId);

    boolean existsByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);
}
