package com.schoolms.security;

import com.schoolms.common.enums.UserStatus;
import com.schoolms.tenant.TenantContext;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates the Bearer access token, stamps the tenant context from the JWT,
 * verifies the user is still active, and builds the authenticated principal.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PermissionCacheService permissionCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractBearerToken(request);
            if (token != null) {
                authenticate(token);
            }
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(String token) {
        Claims claims = jwtService.parseAccessToken(token);
        UUID userId = UUID.fromString(claims.getSubject());
        UUID schoolId = parseSchoolId(claims.get("schoolId", String.class));
        List<String> roles = claims.get("roles", List.class);
        String locale = claims.get("locale", String.class);

        boolean isSuperAdmin = roles != null && roles.contains("SUPER_ADMIN");
        TenantContext.setSchoolId(schoolId);
        TenantContext.setBypassRls(isSuperAdmin);

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty() || user.get().getStatus() != UserStatus.ACTIVE) {
            SecurityContextHolder.clearContext();
            return;
        }

        UserPrincipal principal = new UserPrincipal(
                userId, schoolId, user.get().getUsername(), locale,
                roles == null ? List.of() : roles,
                permissionCacheService.getAuthorities(userId));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private UUID parseSchoolId(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        return UUID.fromString(value);
    }
}
