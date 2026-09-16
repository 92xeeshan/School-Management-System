package com.schoolms.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolEventRepository extends JpaRepository<SchoolEvent, UUID> {

    Optional<SchoolEvent> findByIdAndSchoolId(UUID id, UUID schoolId);

    @Query("""
            select e from SchoolEvent e
            where e.schoolId = :schoolId
              and e.startDate <= :to
              and coalesce(e.endDate, e.startDate) >= :from
            order by e.startDate asc, e.startTime asc nulls first
            """)
    List<SchoolEvent> findInRange(@Param("schoolId") UUID schoolId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);

    @Query("""
            select e from SchoolEvent e
            where e.schoolId = :schoolId
              and e.startDate >= :from
            order by e.startDate asc, e.startTime asc nulls first
            """)
    List<SchoolEvent> findUpcoming(@Param("schoolId") UUID schoolId, @Param("from") LocalDate from);
}
