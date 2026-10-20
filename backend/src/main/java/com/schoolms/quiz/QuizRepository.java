package com.schoolms.quiz;

import com.schoolms.common.enums.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    Optional<Quiz> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<Quiz> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    List<Quiz> findBySchoolIdAndAcademicYearIdOrderByCreatedAtDesc(UUID schoolId, UUID academicYearId);

    List<Quiz> findBySchoolIdAndStatusInOrderByStartAtAscCreatedAtDesc(UUID schoolId, Collection<QuizStatus> statuses);
}
