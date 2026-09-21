package com.schoolms.exam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportCardRepository extends JpaRepository<ReportCard, UUID> {

    Optional<ReportCard> findBySchoolIdAndAcademicYearIdAndStudentIdAndExamTerm(
            UUID schoolId, UUID academicYearId, UUID studentId, String examTerm);

    List<ReportCard> findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
            UUID schoolId, UUID academicYearId, UUID sectionId, String examTerm);
}
