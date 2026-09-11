package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, UUID> {

    List<TimetableEntry> findBySchoolIdAndAcademicYearIdOrderByDayOfWeekAscPeriodNumberAsc(
            UUID schoolId, UUID academicYearId);

    List<TimetableEntry> findBySectionIdAndAcademicYearIdOrderByDayOfWeekAscPeriodNumberAsc(
            UUID sectionId, UUID academicYearId);

    Optional<TimetableEntry> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySectionIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
            UUID sectionId, UUID academicYearId, Short dayOfWeek, Short periodNumber);

    boolean existsBySectionIdAndAcademicYearIdAndDayOfWeekAndPeriodNumberAndIdNot(
            UUID sectionId, UUID academicYearId, Short dayOfWeek, Short periodNumber, UUID id);

    boolean existsByTeacherIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
            UUID teacherId, UUID academicYearId, Short dayOfWeek, Short periodNumber);

    boolean existsByTeacherIdAndAcademicYearIdAndDayOfWeekAndPeriodNumberAndIdNot(
            UUID teacherId, UUID academicYearId, Short dayOfWeek, Short periodNumber, UUID id);

    boolean existsBySchoolIdAndAcademicYearIdAndDayOfWeekAndPeriodNumberAndRoomIgnoreCase(
            UUID schoolId, UUID academicYearId, Short dayOfWeek, Short periodNumber, String room);

    boolean existsBySchoolIdAndAcademicYearIdAndDayOfWeekAndPeriodNumberAndRoomIgnoreCaseAndIdNot(
            UUID schoolId, UUID academicYearId, Short dayOfWeek, Short periodNumber, String room, UUID id);
}
