package com.schoolms.auth;

import com.schoolms.auth.dto.AuthResponse;
import com.schoolms.auth.dto.AuthUserDto;
import com.schoolms.auth.dto.AuthUserViewDto;
import com.schoolms.auth.dto.LoginRequest;
import com.schoolms.auth.dto.RefreshRequest;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.enums.UserStatus;
import com.schoolms.config.JwtProperties;
import com.schoolms.security.PermissionCacheService;
import com.schoolms.security.JwtService;
import com.schoolms.security.SecurityUtils;
import com.schoolms.security.UserPrincipal;
import com.schoolms.tenant.TenantContext;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PermissionCacheService permissionCacheService;
    private final JwtProperties jwtProperties;

    public AuthResponse login(LoginRequest request) {
        AuthUserViewDto authUser = userRepository.findAuthUser(request.username())
                .map(AuthUserViewDto::from)
                .orElseThrow(AuthException::badCredentials);

        if (!passwordEncoder.matches(request.password(), authUser.passwordHash())) {
            throw AuthException.badCredentials();
        }
        if (authUser.status() != UserStatus.ACTIVE) {
            throw AuthException.inactive();
        }
        return issueTokens(authUser.id(), authUser.schoolId(), authUser.username(), authUser.locale());
    }

    public AuthResponse refresh(RefreshRequest request) {
        try {
            // The opaque refresh token is itself the credential for this public
            // endpoint. Look it up with RLS bypassed: we cannot know the owning
            // school until we have loaded the user, and the lookup joins the
            // RLS-protected app_user table.
            TenantContext.setBypassRls(true);

            String tokenHash = jwtService.sha256(request.refreshToken());
            RefreshToken stored = refreshTokenRepository.findByTokenHashFetchUser(tokenHash)
                    .orElseThrow(() -> new BusinessException("auth.invalid_refresh_token"));

            if (stored.getRevokedAt() != null) {
                throw new BusinessException("auth.invalid_refresh_token");
            }
            if (stored.getExpiresAt().isBefore(Instant.now())) {
                throw new BusinessException("auth.refresh_token_expired");
            }

            User user = stored.getUser();
            TenantContext.setSchoolId(user.getSchoolId());
            TenantContext.setBypassRls(user.getSchoolId() == null);

            revokeToken(stored);

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException("auth.user_inactive");
            }
            return issueTokens(user.getId(), user.getSchoolId(), user.getUsername(), user.getLocale());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = jwtService.sha256(refreshToken);
        refreshTokenRepository.findByTokenHashFetchUser(tokenHash).ifPresent(this::revokeToken);
    }

    public AuthUserDto me() {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        return buildUserDto(principal.id());
    }

    private AuthResponse issueTokens(UUID userId, UUID schoolId, String username, String locale) {
        try {
            TenantContext.setSchoolId(schoolId);
            TenantContext.setBypassRls(schoolId == null);

            List<String> roles = userRepository.findRoleCodesByUserId(userId);
            Set<String> permissions = permissionCacheService.getAuthorities(userId);

            String accessToken = jwtService.generateAccessToken(userId, schoolId, roles, locale);
            String refreshToken = persistRefreshToken(userId, schoolId == null);

            userRepository.findById(userId).ifPresent(user -> {
                user.setLastLoginAt(Instant.now());
                userRepository.save(user);
            });

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    Duration.ofMinutes(jwtProperties.accessTokenTtlMinutes()).toSeconds(),
                    new AuthUserDto(userId, schoolId, username, null,
                            null, null, null, locale, roles, permissions));
        } finally {
            TenantContext.clear();
        }
    }

    private String persistRefreshToken(UUID userId, boolean bypassRls) {
        String rawToken = jwtService.generateRefreshToken();
        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(jwtService.sha256(rawToken));
        entity.setExpiresAt(Instant.now().plus(Duration.ofDays(jwtProperties.refreshTokenTtlDays())));
        entity.setUser(userRepository.getReferenceById(userId));
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    private AuthUserDto buildUserDto(UUID userId) {
        try {
            TenantContext.setSchoolId(SecurityUtils.currentPrincipal().schoolId());
            TenantContext.setBypassRls(SecurityUtils.currentPrincipal().schoolId() == null);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("user.not_found"));
            List<String> roles = userRepository.findRoleCodesByUserId(userId);
            Set<String> permissions = permissionCacheService.getAuthorities(userId);
            return new AuthUserDto(
                    user.getId(), user.getSchoolId(), user.getUsername(), user.getEmail(),
                    user.getFirstName(), user.getLastName(), user.getDisplayName(),
                    user.getLocale(), roles, permissions);
        } finally {
            TenantContext.clear();
        }
    }

    private void revokeToken(RefreshToken token) {
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }
}
