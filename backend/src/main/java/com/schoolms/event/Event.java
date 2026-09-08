package com.schoolms.event;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event")
@Getter
@Setter
public class Event extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String type = "OTHER";

    @Column(name = "start_datetime", nullable = false)
    private Instant startDateTime;

    @Column(name = "end_datetime")
    private Instant endDateTime;

    @Column(name = "is_all_day", nullable = false)
    private boolean allDay = false;

    @Column(name = "audience_scope", nullable = false)
    private String audienceScope = "ALL";

    @Column(name = "audience_ref_id")
    private UUID audienceRefId;

    @Column(nullable = false)
    private String source = "MANUAL";

    @Column(name = "sync_status", nullable = false)
    private String syncStatus = "NA";

    @Column(name = "external_provider")
    private String externalProvider;

    @Column(name = "external_ref_id")
    private String externalRefId;

    @Column(name = "created_by")
    private UUID createdBy;
}
