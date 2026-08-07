package com.schoolms.auth;

import java.util.UUID;

/**
 * Projection of the auth_find_user() SECURITY DEFINER function output.
 */
public interface AuthUserView {

    UUID getId();

    UUID getSchoolId();

    String getUsername();

    String getPasswordHash();

    String getStatus();

    String getLocale();

    String getFirstName();

    String getLastName();

    String getEmail();
}
