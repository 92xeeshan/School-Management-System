package com.schoolms.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamEntryRepository extends JpaRepository<ExamEntry, UUID> {

    Optional<ExamEntry> findBySchoolIdAndAcademicYearIdAndSectionIdAndSubjectIdAndExamTerm(
            UUID schoolId, UUID academicYearId, UUID sectionId, UUID subjectId, String examTerm);

    List<ExamEntry> findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
            UUID schoolId, UUID academicYearId, UUID sectionId, String examTerm);

    Optional<ExamEntry> findByIdAndSchoolId(UUID id, UUID schoolId);

    @Query("""
            select count(e) from ExamEntry e
            where e.schoolId = :schoolId and e.locked = false and e.sectionId in :sectionIds
            """)
    long countPendingForSections(@Param("schoolId") UUID schoolId,
                                 @Param("sectionIds") Collection<UUID> sectionIds);
}
