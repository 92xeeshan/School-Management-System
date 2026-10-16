package com.schoolms.student;

import com.schoolms.common.enums.StudentStatus;
import com.schoolms.student.dto.StudentDirectoryRow;
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

    @Query(value = """
            select new com.schoolms.student.dto.StudentDirectoryRow(
                s.id, s.admissionNo, s.firstName, s.lastName, s.gender, s.status, s.admissionDate,
                e.id, e.academicYearId, e.rollNumber,
                sec.id, sec.name, c.id, c.name)
            from Student s
            left join StudentEnrollment e on e.studentId = s.id and e.schoolId = s.schoolId
                 and e.academicYearId = :academicYearId
            left join Section sec on sec.id = e.sectionId and sec.schoolId = s.schoolId
            left join SchoolClass c on c.id = sec.classId and c.schoolId = s.schoolId
            where s.schoolId = :schoolId
              and (:classId is null or c.id = :classId)
              and (:sectionId is null or sec.id = :sectionId)
              and (:status is null or s.status = :status)
              and (:query is null
                   or lower(s.firstName) like :queryLike
                   or lower(s.lastName) like :queryLike
                   or lower(s.admissionNo) like :queryLike
                   or cast(e.rollNumber as string) like :queryLike)
            """,
            countQuery = """
            select count(s) from Student s
            left join StudentEnrollment e on e.studentId = s.id and e.schoolId = s.schoolId
                 and e.academicYearId = :academicYearId
            left join Section sec on sec.id = e.sectionId and sec.schoolId = s.schoolId
            left join SchoolClass c on c.id = sec.classId and c.schoolId = s.schoolId
            where s.schoolId = :schoolId
              and (:classId is null or c.id = :classId)
              and (:sectionId is null or sec.id = :sectionId)
              and (:status is null or s.status = :status)
              and (:query is null
                   or lower(s.firstName) like :queryLike
                   or lower(s.lastName) like :queryLike
                   or lower(s.admissionNo) like :queryLike
                   or cast(e.rollNumber as string) like :queryLike)
            """)
    Page<StudentDirectoryRow> searchDirectory(@Param("schoolId") UUID schoolId,
                                              @Param("academicYearId") UUID academicYearId,
                                              @Param("classId") UUID classId,
                                              @Param("sectionId") UUID sectionId,
                                              @Param("status") StudentStatus status,
                                              @Param("query") String query,
                                              @Param("queryLike") String queryLike,
                                              Pageable pageable);

    List<Student> findBySchoolIdAndIdIn(UUID schoolId, List<UUID> ids);

    long countBySchoolIdAndStatus(UUID schoolId, com.schoolms.common.enums.StudentStatus status);

    @Query("""
            select s.gender as gender, count(s) as total from Student s
            where s.schoolId = :schoolId and s.status = com.schoolms.common.enums.StudentStatus.ACTIVE
            group by s.gender
            """)
    List<GenderCount> countActiveByGender(@Param("schoolId") UUID schoolId);

    interface GenderCount {
        com.schoolms.common.enums.Gender getGender();

        long getTotal();
    }

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
