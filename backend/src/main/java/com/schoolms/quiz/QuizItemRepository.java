package com.schoolms.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizItemRepository extends JpaRepository<QuizItem, UUID> {

    List<QuizItem> findBySchoolIdAndQuizIdOrderBySortOrderAscCreatedAtAsc(UUID schoolId, UUID quizId);

    List<QuizItem> findBySchoolIdAndQuizIdInOrderBySortOrderAscCreatedAtAsc(UUID schoolId, Collection<UUID> quizIds);

    Optional<QuizItem> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsByQuizIdAndQuestionId(UUID quizId, UUID questionId);

    boolean existsByQuestionId(UUID questionId);

    long countByQuizId(UUID quizId);

    void deleteByQuizIdAndSchoolId(UUID quizId, UUID schoolId);
}
