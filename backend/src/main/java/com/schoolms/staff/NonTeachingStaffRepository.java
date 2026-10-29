package com.schoolms.staff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NonTeachingStaffRepository extends JpaRepository<NonTeachingStaff, UUID> {

    List<NonTeachingStaff> findBySchoolIdOrderByFirstNameAsc(UUID schoolId);

    Page<NonTeachingStaff> findBySchoolId(UUID schoolId, Pageable pageable);

    @Query("""
            SELECT s FROM NonTeachingStaff s
            WHERE s.schoolId = :schoolId
              AND (:query IS NULL
                OR LOWER(s.firstName) LIKE :like
                OR LOWER(COALESCE(s.lastName, '')) LIKE :like
                OR LOWER(s.employeeNo) LIKE :like
                OR LOWER(COALESCE(s.designation, '')) LIKE :like
                OR LOWER(COALESCE(s.department, '')) LIKE :like
                OR LOWER(COALESCE(s.phone, '')) LIKE :like)
            """)
    Page<NonTeachingStaff> searchDirectory(@Param("schoolId") UUID schoolId,
                                           @Param("query") String query,
                                           @Param("like") String like,
                                           Pageable pageable);

    Optional<NonTeachingStaff> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<NonTeachingStaff> findBySchoolIdAndUserId(UUID schoolId, UUID userId);

    boolean existsBySchoolIdAndEmployeeNo(UUID schoolId, String employeeNo);

    boolean existsBySchoolIdAndEmployeeNoAndIdNot(UUID schoolId, String employeeNo, UUID id);

    @Query("""
            select s.employeeNo from NonTeachingStaff s
            where s.schoolId = :schoolId and s.employeeNo like concat(:prefix, '%')
            """)
    List<String> findEmployeeNosByPrefix(@Param("schoolId") UUID schoolId, @Param("prefix") String prefix);
}
