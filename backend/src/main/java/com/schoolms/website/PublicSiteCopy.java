package com.schoolms.website;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "public_site_copy")
@Getter
@Setter
public class PublicSiteCopy extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(length = 300)
    private String tagline;

    @Column(columnDefinition = "text")
    private String about;

    @Column(name = "principal_message", columnDefinition = "text")
    private String principalMessage;

    @Column(columnDefinition = "text")
    private String facilities;
}
