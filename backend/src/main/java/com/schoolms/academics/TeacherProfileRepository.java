package com.schoolms.academics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {

    List<TeacherProfile> findBySchoolIdOrderByFirstNameAsc(UUID schoolId);

    Page<TeacherProfile> findBySchoolId(UUID schoolId, Pageable pageable);

    @Query("""
            SELECT t FROM TeacherProfile t
            WHERE t.schoolId = :schoolId
              AND (:query IS NULL
                OR LOWER(t.firstName) LIKE :like
                OR LOWER(COALESCE(t.lastName, '')) LIKE :like
                OR LOWER(t.employeeNo) LIKE :like
                OR LOWER(COALESCE(t.designation, '')) LIKE :like
                OR LOWER(COALESCE(t.department, '')) LIKE :like)
            """)
    Page<TeacherProfile> searchDirectory(@Param("schoolId") UUID schoolId,
                                         @Param("query") String query,
                                         @Param("like") String like,
                                         Pageable pageable);

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
