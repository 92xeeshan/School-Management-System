package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {

    Optional<StudentEnrollment> findByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);

    List<StudentEnrollment> findBySectionIdAndAcademicYearId(UUID sectionId, UUID academicYearId);

    List<StudentEnrollment> findByStudentId(UUID studentId);

    List<StudentEnrollment> findBySchoolIdAndAcademicYearIdAndStatus(
            UUID schoolId, UUID academicYearId, String status);

    List<StudentEnrollment> findBySchoolIdAndAcademicYearIdAndStatusAndSectionId(
            UUID schoolId, UUID academicYearId, String status, UUID sectionId);

    @Query("""
            select e from StudentEnrollment e
            where e.schoolId = :schoolId
              and e.academicYearId = :academicYearId
              and e.status = :status
              and e.sectionId in :sectionIds
            """)
    List<StudentEnrollment> findBySchoolIdAndAcademicYearIdAndStatusAndSectionIdIn(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId,
            @Param("status") String status,
            @Param("sectionIds") List<UUID> sectionIds);

    long countBySectionIdAndAcademicYearIdAndStatus(UUID sectionId, UUID academicYearId, String status);

    boolean existsByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);
}
