package com.schoolms.auth;

import com.schoolms.auth.dto.AuthResponse;
import com.schoolms.auth.dto.LoginRequest;
import com.schoolms.certificate.CertificateLifecycleService;
import com.schoolms.common.exception.AuthException;
import com.schoolms.config.JwtProperties;
import com.schoolms.security.JwtService;
import com.schoolms.security.PermissionCacheService;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.tenant.TenantBinder;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private PermissionCacheService permissionCacheService;
    @Mock private JwtProperties jwtProperties;
    @Mock private CertificateLifecycleService certificateLifecycleService;
    @Mock private GuardianRepository guardianRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private TenantBinder tenantBinder;

    private AuthService authService;

    private final UUID userId = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private final UUID schoolId = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                jwtService, permissionCacheService, jwtProperties, certificateLifecycleService,
                guardianRepository, studentGuardianRepository, tenantBinder);
    }

    @AfterEach
    void tearDown() {
        com.schoolms.tenant.TenantContext.clear();
    }

    @Test
    void loginSuccessIssuesTokens() {
        AuthUserView authUser = new StubAuthUser(userId, schoolId, "admin",
                "hash", "ACTIVE", "en");
        User stored = new User();
        stored.setId(userId);
        stored.setSchoolId(schoolId);
        stored.setUsername("admin");
        stored.setFirstName("Admin");
        stored.setLocale("en");
        stored.setTokenVersion(0);
        when(userRepository.findAuthUser("admin")).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("Admin@123", "hash")).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(stored));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findRoleCodesByUserId(userId)).thenReturn(List.of("ADMIN"));
        when(permissionCacheService.getAuthorities(userId)).thenReturn(Set.of("STUDENT_READ"));
        when(jwtService.generateAccessToken(eq(userId), eq(schoolId), eq(List.of("ADMIN")), eq("en"), anyInt()))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtService.sha256("refresh-token")).thenReturn("refresh-hash");
        when(jwtProperties.accessTokenTtlMinutes()).thenReturn(15L);
        when(jwtProperties.refreshTokenTtlDays()).thenReturn(30L);
        when(userRepository.getReferenceById(userId)).thenReturn(stored);

        AuthResponse response = authService.login(new LoginRequest("admin", "Admin@123"));

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(900, response.expiresInSeconds());
        verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void loginWithWrongPasswordRejects() {
        AuthUserView authUser = new StubAuthUser(userId, schoolId, "admin",
                "hash", "ACTIVE", "en");
        User stored = new User();
        stored.setId(userId);
        stored.setFailedLoginAttempts(0);
        when(userRepository.findAuthUser("admin")).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(stored));

        assertThrows(AuthException.class,
                () -> authService.login(new LoginRequest("admin", "wrong")));
    }

    @Test
    void loginWithInactiveUserRejects() {
        AuthUserView authUser = new StubAuthUser(userId, schoolId, "admin",
                "hash", "INACTIVE", "en");
        when(userRepository.findAuthUser("admin")).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("Admin@123", "hash")).thenReturn(true);
        when(certificateLifecycleService.deactivatedForTc(schoolId, userId)).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.login(new LoginRequest("admin", "Admin@123")));
        assertEquals("auth.user_inactive", ex.getCode());
    }

    @Test
    void loginWithTransferCertificateDeactivatedRejects() {
        AuthUserView authUser = new StubAuthUser(userId, schoolId, "student",
                "hash", "INACTIVE", "en");
        when(userRepository.findAuthUser("student")).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("Admin@123", "hash")).thenReturn(true);
        when(certificateLifecycleService.deactivatedForTc(schoolId, userId)).thenReturn(true);

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.login(new LoginRequest("student", "Admin@123")));
        assertEquals("auth.tc_deactivated", ex.getCode());
    }

    @Test
    void loginWithUnknownUserRejects() {
        when(userRepository.findAuthUser("nobody")).thenReturn(Optional.empty());

        assertThrows(AuthException.class,
                () -> authService.login(new LoginRequest("nobody", "Admin@123")));
    }

    @Test
    void loginLocksAfterFiveFailures() {
        AuthUserView authUser = new StubAuthUser(userId, schoolId, "admin",
                "hash", "ACTIVE", "en");
        User stored = new User();
        stored.setId(userId);
        stored.setFailedLoginAttempts(4);
        when(userRepository.findAuthUser("admin")).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(stored));

        assertThrows(AuthException.class,
                () -> authService.login(new LoginRequest("admin", "wrong")));
        assertEquals(5, stored.getFailedLoginAttempts());
        assertEquals(com.schoolms.common.enums.UserStatus.LOCKED, stored.getStatus());
    }

    record StubAuthUser(UUID id, UUID schoolId, String username, String passwordHash,
                        String status, String locale) implements AuthUserView {
        @Override public UUID getId() { return id; }
        @Override public UUID getSchoolId() { return schoolId; }
        @Override public String getUsername() { return username; }
        @Override public String getPasswordHash() { return passwordHash; }
        @Override public String getStatus() { return status; }
        @Override public String getLocale() { return locale; }
        @Override public String getFirstName() { return null; }
        @Override public String getLastName() { return null; }
        @Override public String getEmail() { return null; }
        @Override public Boolean getMustChangePassword() { return false; }
        @Override public Boolean getQuestionsSet() { return false; }
        @Override public Integer getTokenVersion() { return 0; }
        @Override public Integer getFailedLoginAttempts() { return 0; }
        @Override public Instant getLockedUntil() { return null; }
    }
}
