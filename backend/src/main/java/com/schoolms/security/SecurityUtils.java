package com.schoolms.security;

import com.schoolms.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException("auth.unauthorized");
        }
        return principal;
    }

    public static UUID currentUserId() {
        return currentPrincipal().id();
    }

    /** The tenant (school) the current user belongs to. */
    public static UUID currentSchoolId() {
        UUID schoolId = currentPrincipal().schoolId();
        if (schoolId == null) {
            throw new BusinessException("auth.no_school_context");
        }
        return schoolId;
    }
}
