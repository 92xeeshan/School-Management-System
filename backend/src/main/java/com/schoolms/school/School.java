package com.schoolms.school;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.SchoolStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "school")
@Getter
@Setter
public class School extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String address;

    private String phone;

    private String email;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private String currency = "INR";

    @Column(name = "default_locale", nullable = false)
    private String defaultLocale = "en";

    @Column(nullable = false)
    private String timezone = "Asia/Kolkata";

    @Column(name = "academic_start_month", nullable = false)
    private Short academicStartMonth = 4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchoolStatus status = SchoolStatus.ACTIVE;
}
