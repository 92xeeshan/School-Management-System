package com.schoolms.quiz;

import com.schoolms.common.enums.QuizAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<QuizAttempt> findBySchoolIdAndQuizIdOrderByAttemptNoAsc(UUID schoolId, UUID quizId);

    List<QuizAttempt> findBySchoolIdAndQuizIdIn(UUID schoolId, Collection<UUID> quizIds);

    List<QuizAttempt> findBySchoolIdAndStudentIdAndQuizIdOrderByAttemptNoAsc(UUID schoolId, UUID studentId, UUID quizId);

    List<QuizAttempt> findBySchoolIdAndStudentIdOrderByStartedAtDesc(UUID schoolId, UUID studentId);

    Optional<QuizAttempt> findFirstBySchoolIdAndQuizIdAndStudentIdAndStatus(
            UUID schoolId, UUID quizId, UUID studentId, QuizAttemptStatus status);

    long countByQuizIdAndStudentId(UUID quizId, UUID studentId);
}
