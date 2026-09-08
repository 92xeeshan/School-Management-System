package com.schoolms.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findBySchoolIdOrderByStartDateTimeAsc(UUID schoolId);

    Optional<Event> findByIdAndSchoolId(UUID id, UUID schoolId);
}
