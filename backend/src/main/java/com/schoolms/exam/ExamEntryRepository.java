package com.schoolms.exam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExamEntryRepository extends JpaRepository<ExamEntry, UUID> {

    Optional<ExamEntry> findBySchoolIdAndAcademicYearIdAndSectionIdAndSubjectIdAndExamTerm(
            UUID schoolId, UUID academicYearId, UUID sectionId, UUID subjectId, String examTerm);

    Optional<ExamEntry> findByIdAndSchoolId(UUID id, UUID schoolId);
}
