package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {

    List<TeacherProfile> findBySchoolIdOrderByFirstNameAsc(UUID schoolId);

    Optional<TeacherProfile> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<TeacherProfile> findBySchoolIdAndUserId(UUID schoolId, UUID userId);

    boolean existsBySchoolIdAndEmployeeNo(UUID schoolId, String employeeNo);

    boolean existsBySchoolIdAndEmployeeNoAndIdNot(UUID schoolId, String employeeNo, UUID id);

    @Query("""
            select t.employeeNo from TeacherProfile t
            where t.schoolId = :schoolId and t.employeeNo like concat(:prefix, '%')
            """)
    List<String> findEmployeeNosByPrefix(@Param("schoolId") UUID schoolId, @Param("prefix") String prefix);
}
