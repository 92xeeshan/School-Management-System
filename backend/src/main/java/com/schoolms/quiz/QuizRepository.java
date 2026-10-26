package com.schoolms.quiz;

import com.schoolms.common.enums.QuizStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    Optional<Quiz> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<Quiz> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    List<Quiz> findBySchoolIdAndAcademicYearIdOrderByCreatedAtDesc(UUID schoolId, UUID academicYearId);

    List<Quiz> findBySchoolIdAndStatusInOrderByStartAtAscCreatedAtDesc(UUID schoolId, Collection<QuizStatus> statuses);

    @Query("""
            SELECT q FROM Quiz q
            WHERE q.schoolId = :schoolId
              AND (:academicYearId IS NULL OR q.academicYearId = :academicYearId)
              AND (:classId IS NULL OR q.classId = :classId)
              AND (:sectionId IS NULL OR q.sectionId = :sectionId OR q.sectionId IS NULL)
              AND (:subjectId IS NULL OR q.subjectId = :subjectId)
              AND (:status IS NULL OR q.status = :status)
              AND (:like IS NULL OR LOWER(q.title) LIKE :like)
            """)
    Page<Quiz> searchDirectory(@Param("schoolId") UUID schoolId,
                               @Param("academicYearId") UUID academicYearId,
                               @Param("classId") UUID classId,
                               @Param("sectionId") UUID sectionId,
                               @Param("subjectId") UUID subjectId,
                               @Param("status") QuizStatus status,
                               @Param("like") String like,
                               Pageable pageable);
}
