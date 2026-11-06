package com.schoolms.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentPreviousSchoolRepository extends JpaRepository<StudentPreviousSchool, UUID> {

    Optional<StudentPreviousSchool> findByStudentIdAndSchoolId(UUID studentId, UUID schoolId);
}
