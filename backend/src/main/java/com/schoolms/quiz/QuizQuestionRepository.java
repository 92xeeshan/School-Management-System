package com.schoolms.quiz;

import com.schoolms.common.enums.QuizQuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
