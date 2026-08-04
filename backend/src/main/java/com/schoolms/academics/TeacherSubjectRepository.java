package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, UUID> {

    List<TeacherSubject> findByTeacherIdAndSchoolId(UUID teacherId, UUID schoolId);

    boolean existsByTeacherIdAndSubjectId(UUID teacherId, UUID subjectId);

    void deleteByTeacherIdAndSchoolId(UUID teacherId, UUID schoolId);
}
