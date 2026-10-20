package com.schoolms.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizOfflineMarkRepository extends JpaRepository<QuizOfflineMark, UUID> {

    List<QuizOfflineMark> findBySchoolIdAndQuizId(UUID schoolId, UUID quizId);

    Optional<QuizOfflineMark> findByQuizIdAndStudentId(UUID quizId, UUID studentId);
}
