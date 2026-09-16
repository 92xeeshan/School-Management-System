package com.schoolms.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamMarkRepository extends JpaRepository<ExamMark, UUID> {

    List<ExamMark> findByExamEntryIdAndSchoolId(UUID examEntryId, UUID schoolId);

    Optional<ExamMark> findByExamEntryIdAndStudentId(UUID examEntryId, UUID studentId);

    @Query(value = """
            select avg(m.percentage)
            from exam_mark m
            join exam_entry e on e.id = m.exam_entry_id
            where e.school_id = :schoolId and e.section_id = :sectionId and m.percentage is not null
            """, nativeQuery = true)
    Double avgPercentageBySection(@Param("schoolId") UUID schoolId,
                                  @Param("sectionId") UUID sectionId);
}
