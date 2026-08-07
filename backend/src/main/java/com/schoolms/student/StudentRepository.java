package com.schoolms.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    boolean existsBySchoolIdAndAdmissionNo(UUID schoolId, String admissionNo);

    Optional<Student> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<Student> findBySchoolIdAndUserId(UUID schoolId, UUID userId);

    Page<Student> findBySchoolId(UUID schoolId, Pageable pageable);

    @Query("""
            select s from Student s
            where s.schoolId = :schoolId
              and (:query is null or lower(s.firstName) like lower(concat('%', :query, '%'))
                   or lower(s.lastName) like lower(concat('%', :query, '%'))
                   or lower(s.admissionNo) like lower(concat('%', :query, '%')))
            """)
    Page<Student> search(@Param("schoolId") UUID schoolId,
                         @Param("query") String query,
                         Pageable pageable);

    List<Student> findBySchoolIdAndIdIn(UUID schoolId, List<UUID> ids);

    long countBySchoolIdAndStatus(UUID schoolId, com.schoolms.common.enums.StudentStatus status);

    @Query(value = """
            select s.* from student s
            join student_enrollment e on e.student_id = s.id
            where s.school_id = :schoolId and e.section_id = :sectionId
              and e.academic_year_id = :academicYearId and e.status = 'ACTIVE'
            order by e.roll_number nulls last, s.last_name
            """, nativeQuery = true)
    List<Student> findBySectionAndYear(@Param("schoolId") UUID schoolId,
                                       @Param("sectionId") UUID sectionId,
                                       @Param("academicYearId") UUID academicYearId);
}
