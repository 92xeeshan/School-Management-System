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

    @Query("select sg from StudentGuardian sg join fetch sg.guardian where sg.student.schoolId = :schoolId and sg.student.id = :studentId")
    List<StudentGuardian> findWithGuardians(@Param("schoolId") UUID schoolId, @Param("studentId") UUID studentId);

    @Query("select sg from StudentGuardian sg where sg.guardian.schoolId = :schoolId and sg.guardian.id = :guardianId")
    List<StudentGuardian> findWithStudents(@Param("schoolId") UUID schoolId, @Param("guardianId") UUID guardianId);

    @Query("""
            select distinct sg2.student.id from StudentGuardian sg1
            join StudentGuardian sg2 on sg2.guardian.id = sg1.guardian.id
            where sg1.student.id = :studentId
              and sg1.schoolId = :schoolId
              and sg2.schoolId = :schoolId
            """)
    List<UUID> findSiblingStudentIds(@Param("schoolId") UUID schoolId, @Param("studentId") UUID studentId);
}
