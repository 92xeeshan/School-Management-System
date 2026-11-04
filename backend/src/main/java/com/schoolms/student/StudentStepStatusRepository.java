package com.schoolms.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentStepStatusRepository extends JpaRepository<StudentStepStatus, UUID> {

    List<StudentStepStatus> findByStudentIdAndSchoolIdOrderByStepNoAsc(UUID studentId, UUID schoolId);

    Optional<StudentStepStatus> findByStudentIdAndStepNo(UUID studentId, int stepNo);
}
