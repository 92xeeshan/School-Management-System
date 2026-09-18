package com.schoolms.exam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, UUID> {

    List<ExamSchedule> findBySchoolIdAndAcademicYearIdOrderByExamDateAscStartTimeAsc(
            UUID schoolId, UUID academicYearId);

    List<ExamSchedule> findBySchoolIdAndExamDate(UUID schoolId, LocalDate examDate);

    Optional<ExamSchedule> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndAcademicYearIdAndSectionIdAndSubjectIdAndExamTerm(
            UUID schoolId, UUID academicYearId, UUID sectionId, UUID subjectId, String examTerm);

    boolean existsBySchoolIdAndAcademicYearIdAndSectionIdAndSubjectIdAndExamTermAndIdNot(
            UUID schoolId, UUID academicYearId, UUID sectionId, UUID subjectId, String examTerm, UUID id);
}
