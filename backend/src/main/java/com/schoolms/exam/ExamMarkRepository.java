package com.schoolms.exam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamMarkRepository extends JpaRepository<ExamMark, UUID> {

    List<ExamMark> findByExamEntryIdAndSchoolId(UUID examEntryId, UUID schoolId);

    Optional<ExamMark> findByExamEntryIdAndStudentId(UUID examEntryId, UUID studentId);
}
