package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, UUID> {

    List<TimetableEntry> findBySectionIdAndAcademicYearIdOrderByDayOfWeekAscPeriodNumberAsc(
            UUID sectionId, UUID academicYearId);

    Optional<TimetableEntry> findBySectionIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
            UUID sectionId, UUID academicYearId, Short dayOfWeek, Short periodNumber);

    boolean existsBySectionIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
            UUID sectionId, UUID academicYearId, Short dayOfWeek, Short periodNumber);
}
