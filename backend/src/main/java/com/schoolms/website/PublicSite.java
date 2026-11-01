package com.schoolms.website;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "public_site")
@Getter
@Setter
public class PublicSite extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false)
    private boolean published = true;

    @Column(name = "hero_image_url", length = 500)
    private String heroImageUrl;

    @Column(name = "principal_name", length = 120)
    private String principalName;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
