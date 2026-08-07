package com.schoolms.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByStudentIdAndAttendanceDate(UUID studentId, LocalDate date);

    List<Attendance> findBySectionIdAndAttendanceDateOrderByStudentIdAsc(UUID sectionId, LocalDate date);

    List<Attendance> findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            UUID studentId, LocalDate from, LocalDate to);

    List<Attendance> findBySectionIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            UUID sectionId, LocalDate from, LocalDate to);

    @Query("""
            select count(a) from Attendance a
            where a.studentId = :studentId and a.attendanceDate between :from and :to
            """)
    long countByStudentBetween(@Param("studentId") UUID studentId,
                               @Param("from") LocalDate from,
                               @Param("to") LocalDate to);

    @Query("""
            select count(a) from Attendance a
            where a.studentId = :studentId and a.status = com.schoolms.common.enums.AttendanceStatus.PRESENT
              and a.attendanceDate between :from and :to
            """)
    long countPresentByStudentBetween(@Param("studentId") UUID studentId,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    long countBySchoolIdAndAttendanceDateAndStatus(UUID schoolId, LocalDate date,
                                                   com.schoolms.common.enums.AttendanceStatus status);
}
