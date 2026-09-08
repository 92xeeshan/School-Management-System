package com.schoolms;

import com.schoolms.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Test helper that seeds the security context with a tenant-scoped principal. */
public final class TestSecurity {

    public static final UUID SCHOOL_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private TestSecurity() {
    }

    public static void login(UUID userId, UUID schoolId, List<String> roles, Set<String> permissions) {
        UserPrincipal principal = new UserPrincipal(userId, schoolId, "test", "en", roles, permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    public static void loginAsAdmin() {
        login(USER_ID, SCHOOL_ID, List.of("ADMIN"), Set.of("STUDENT_READ", "STUDENT_UPDATE", "CLASS_READ"));
    }

    public static void loginAsTeacher() {
        login(USER_ID, SCHOOL_ID, List.of("TEACHER"), Set.of("STUDENT_READ"));
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
