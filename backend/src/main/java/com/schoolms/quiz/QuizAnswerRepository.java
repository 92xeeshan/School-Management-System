package com.schoolms.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, UUID> {

    List<QuizAnswer> findBySchoolIdAndAttemptId(UUID schoolId, UUID attemptId);

    List<QuizAnswer> findBySchoolIdAndAttemptIdIn(UUID schoolId, Collection<UUID> attemptIds);

    Optional<QuizAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}
