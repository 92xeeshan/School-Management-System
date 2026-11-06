package com.schoolms.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentSubjectRepository extends JpaRepository<StudentSubject, UUID> {

    List<StudentSubject> findByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);

    void deleteByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);
}
