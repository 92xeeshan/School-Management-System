package com.schoolms.user;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class User extends BaseEntity {

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(nullable = false)
    private String username;

    private String email;

    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false)
    private String locale = "en";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public String getDisplayName() {
        return lastName == null || lastName.isBlank()
                ? firstName
                : firstName + " " + lastName;
    }
}
