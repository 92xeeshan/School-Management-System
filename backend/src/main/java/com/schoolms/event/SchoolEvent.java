package com.schoolms.event;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.EventType;
import com.schoolms.common.enums.EventVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "school_event")
@Getter
@Setter
public class SchoolEvent extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType = EventType.EVENT;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "all_day", nullable = false)
    private boolean allDay = true;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", nullable = false)
    private EventVisibility visibilityScope = EventVisibility.SCHOOL_WIDE;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "created_by")
    private UUID createdBy;
}
