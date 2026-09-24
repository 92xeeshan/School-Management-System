package com.schoolms.exam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarksheetRepository extends JpaRepository<Marksheet, UUID> {

    Optional<Marksheet> findBySchoolIdAndAcademicYearIdAndStudentIdAndExamTerm(
            UUID schoolId, UUID academicYearId, UUID studentId, String examTerm);

    List<Marksheet> findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
            UUID schoolId, UUID academicYearId, UUID sectionId, String examTerm);

    List<Marksheet> findBySchoolIdAndAcademicYearIdAndExamTermAndStudentIdIn(
            UUID schoolId, UUID academicYearId, String examTerm, Collection<UUID> studentIds);
}
