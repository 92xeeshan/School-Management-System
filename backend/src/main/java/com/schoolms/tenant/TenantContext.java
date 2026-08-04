package com.schoolms.tenant;

import java.util.UUID;

/**
 * Holds the tenant context (school id + RLS bypass flag) for the current
 * thread. Populated from the JWT by the security filter and applied to every
 * JDBC connection checked out by {@link TenantAwareHikariDataSource}.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> SCHOOL_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> BYPASS_RLS = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setSchoolId(UUID schoolId) {
        SCHOOL_ID.set(schoolId);
    }

    public static UUID getSchoolId() {
        return SCHOOL_ID.get();
    }

    public static void setBypassRls(boolean bypass) {
        BYPASS_RLS.set(bypass);
    }

    public static boolean isBypassRls() {
        return Boolean.TRUE.equals(BYPASS_RLS.get());
    }

    public static void clear() {
        SCHOOL_ID.remove();
        BYPASS_RLS.remove();
    }
}
