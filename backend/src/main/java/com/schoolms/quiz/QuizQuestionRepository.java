package com.schoolms.quiz;

import com.schoolms.common.enums.QuizQuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    Optional<QuizQuestion> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<QuizQuestion> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    List<QuizQuestion> findBySchoolIdAndIdIn(UUID schoolId, Collection<UUID> ids);

    List<QuizQuestion> findBySchoolIdAndSubjectIdOrderByCreatedAtDesc(UUID schoolId, UUID subjectId);

    List<QuizQuestion> findBySchoolIdAndClassIdOrderByCreatedAtDesc(UUID schoolId, UUID classId);

    List<QuizQuestion> findBySchoolIdAndQuestionTypeOrderByCreatedAtDesc(UUID schoolId, QuizQuestionType questionType);

    @Query("""
            SELECT q FROM QuizQuestion q
            WHERE q.schoolId = :schoolId
              AND (:subjectId IS NULL OR q.subjectId = :subjectId)
              AND (:classId IS NULL OR q.classId = :classId)
              AND (:questionType IS NULL OR q.questionType = :questionType)
              AND (:like IS NULL OR LOWER(q.prompt) LIKE :like)
            """)
    Page<QuizQuestion> searchDirectory(@Param("schoolId") UUID schoolId,
                                       @Param("subjectId") UUID subjectId,
                                       @Param("classId") UUID classId,
                                       @Param("questionType") QuizQuestionType questionType,
                                       @Param("like") String like,
                                       Pageable pageable);
}
