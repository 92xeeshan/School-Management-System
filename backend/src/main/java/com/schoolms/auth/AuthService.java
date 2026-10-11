package com.schoolms.auth;

import com.schoolms.auth.dto.AuthResponse;
import com.schoolms.auth.dto.AuthUserDto;
import com.schoolms.auth.dto.AuthUserViewDto;
import com.schoolms.auth.dto.LinkedChildDto;
import com.schoolms.auth.dto.LoginRequest;
import com.schoolms.auth.dto.RefreshRequest;
import com.schoolms.certificate.CertificateLifecycleService;
import com.schoolms.common.enums.UserStatus;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.config.JwtProperties;
import com.schoolms.security.JwtService;
import com.schoolms.security.PermissionCacheService;
import com.schoolms.security.SecurityUtils;
import com.schoolms.security.UserPrincipal;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.tenant.TenantBinder;
import com.schoolms.tenant.TenantContext;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
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
    private final CertificateLifecycleService certificateLifecycleService;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final TenantBinder tenantBinder;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AuthUserViewDto authUser = userRepository.findAuthUser(request.username())
                .map(AuthUserViewDto::from)
                .orElseThrow(AuthException::badCredentials);

        if (authUser.lockedUntil() != null && authUser.lockedUntil().isAfter(Instant.now())) {
            throw AuthException.locked();
        }
        if (authUser.status() == UserStatus.LOCKED) {
            throw AuthException.locked();
        }
        tenantBinder.bind(authUser.schoolId());
        if (!passwordEncoder.matches(request.password(), authUser.passwordHash())) {
            registerFailedLogin(authUser.id(), authUser.schoolId());
            throw AuthException.badCredentials();
        }
        if (authUser.status() != UserStatus.ACTIVE) {
            if (certificateLifecycleService.deactivatedForTc(authUser.schoolId(), authUser.id())) {
                throw AuthException.transferCertificateInactive();
            }
            throw AuthException.inactive();
        }
        clearFailedLogins(authUser.id(), authUser.schoolId());
        return issueTokens(authUser.id(), authUser.schoolId(), true);
    }

    public AuthResponse refresh(RefreshRequest request) {
        try {
            tenantBinder.bindBypass();

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
            return issueTokens(user.getId(), user.getSchoolId(), false);
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
        return buildUserDto(SecurityUtils.currentUserId());
    }

    @Transactional(readOnly = true)
    public List<LinkedChildDto> myChildren() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        return guardianRepository.findBySchoolIdAndUserId(schoolId, userId)
                .map(guardian -> studentGuardianRepository.findWithStudents(schoolId, guardian.getId()).stream()
                        .map(sg -> new LinkedChildDto(
                                sg.getStudent().getId(),
                                sg.getStudent().getDisplayName(),
                                sg.getStudent().getAdmissionNo(),
                                null,
                                null))
                        .toList())
                .orElse(List.of());
    }

    private AuthResponse issueTokens(UUID userId, UUID schoolId, boolean kickOtherSessions) {
        try {
            tenantBinder.bind(schoolId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("user.not_found"));
            if (kickOtherSessions) {
                user.setTokenVersion(user.getTokenVersion() + 1);
                refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
            }
            user.setLastLoginAt(Instant.now());
            user = userRepository.save(user);

            List<String> roles = userRepository.findRoleCodesByUserId(userId);
            Set<String> permissions = permissionCacheService.getAuthorities(userId);
            String accessToken = jwtService.generateAccessToken(
                    userId, schoolId, roles, user.getLocale(), user.getTokenVersion());
            String refreshToken = persistRefreshToken(userId);

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    Duration.ofMinutes(jwtProperties.accessTokenTtlMinutes()).toSeconds(),
                    toAuthUserDto(user, roles, permissions));
        } finally {
            TenantContext.clear();
        }
    }

    private String persistRefreshToken(UUID userId) {
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
            return toAuthUserDto(user, roles, permissions);
        } finally {
            TenantContext.clear();
        }
    }

    private AuthUserDto toAuthUserDto(User user, List<String> roles, Set<String> permissions) {
        return new AuthUserDto(
                user.getId(), user.getSchoolId(), user.getUsername(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getDisplayName(),
                user.getLocale(), roles, permissions,
                user.isMustChangePassword(), user.isQuestionsSet());
    }

    private void registerFailedLogin(UUID userId, UUID schoolId) {
        try {
            tenantBinder.bind(schoolId);
            userRepository.findById(userId).ifPresent(user -> {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= CredentialService.MAX_FAILED_ATTEMPTS) {
                    user.setLockedUntil(Instant.now().plus(CredentialService.LOCKOUT));
                    user.setStatus(UserStatus.LOCKED);
                }
                userRepository.save(user);
            });
        } finally {
            TenantContext.clear();
        }
    }

    private void clearFailedLogins(UUID userId, UUID schoolId) {
        try {
            tenantBinder.bind(schoolId);
            userRepository.findById(userId).ifPresent(user -> {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            });
        } finally {
            TenantContext.clear();
        }
    }

    private void revokeToken(RefreshToken token) {
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }
}
