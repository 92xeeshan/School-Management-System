package com.schoolms.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, UUID> {

    List<StudentGuardian> findByStudentIdAndSchoolId(UUID studentId, UUID schoolId);

    List<StudentGuardian> findByGuardianIdAndSchoolId(UUID guardianId, UUID schoolId);

    Optional<StudentGuardian> findByStudentIdAndGuardianId(UUID studentId, UUID guardianId);

    boolean existsByStudentIdAndGuardianId(UUID studentId, UUID guardianId);

    @Query("select sg from StudentGuardian sg where sg.student.schoolId = :schoolId and sg.student.id = :studentId")
    List<StudentGuardian> findWithGuardians(@Param("schoolId") UUID schoolId, @Param("studentId") UUID studentId);

    @Query("select sg from StudentGuardian sg where sg.guardian.schoolId = :schoolId and sg.guardian.id = :guardianId")
    List<StudentGuardian> findWithStudents(@Param("schoolId") UUID schoolId, @Param("guardianId") UUID guardianId);
}
