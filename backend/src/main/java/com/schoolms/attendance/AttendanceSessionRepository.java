package com.schoolms.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {

    Optional<AttendanceSession> findBySectionIdAndAcademicYearIdAndAttendanceDateAndSubjectId(
            UUID sectionId, UUID academicYearId, LocalDate date, UUID subjectId);

    Optional<AttendanceSession> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<AttendanceSession> findBySectionIdAndAcademicYearIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            UUID sectionId, UUID academicYearId, LocalDate from, LocalDate to);

    boolean existsBySectionIdAndAcademicYearIdAndAttendanceDate(
            UUID sectionId, UUID academicYearId, LocalDate date);
}
