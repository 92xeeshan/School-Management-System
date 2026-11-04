package com.schoolms.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, UUID> {

    List<StudentDocument> findByStudentIdAndSchoolIdOrderByCreatedAtDesc(UUID studentId, UUID schoolId);

    Optional<StudentDocument> findByIdAndSchoolId(UUID id, UUID schoolId);
}
