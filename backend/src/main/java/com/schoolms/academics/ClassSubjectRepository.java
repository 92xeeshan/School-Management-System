package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, UUID> {

    List<ClassSubject> findBySchoolId(UUID schoolId);

    List<ClassSubject> findByClassIdAndSchoolId(UUID classId, UUID schoolId);

    boolean existsByClassIdAndSubjectId(UUID classId, UUID subjectId);
}
