package com.schoolms.auth;

import com.schoolms.auth.dto.AuthResponse;
import com.schoolms.auth.dto.LoginRequest;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.enums.UserStatus;
import com.schoolms.config.JwtProperties;
import com.schoolms.security.JwtService;
import com.schoolms.security.PermissionCacheService;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private PermissionCacheService permissionCacheService;
    @Mock
    private JwtProperties jwtProperties;

    private AuthService authService;

    private final UUID userId = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private final UUID schoolId = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                jwtService, permissionCacheService, jwtProperties);
    }

    @AfterEach
    void tearDown() {
        com.schoolms.tenant.TenantContext.clear();
    }

    @Test
    void loginSuccessIssuesTokens() {
        AuthUserView authUser = new StubAuthUser(userId, schoolId, "admin",
                "hash", "ACTIVE", "en");
        when(userRepository.findAuthUser("admin")).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("Admin@123", "hash")).thenReturn(true);
        when(userRepository.findRoleCodesByUserId(userId)).thenReturn(List.of("ADMIN"));
        when(permissionCacheService.getAuthorities(userId)).thenReturn(Set.of("STUDENT_READ"));
        when(jwtService.generateAccessToken(userId, schoolId, List.of("ADMIN"), "en"))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtService.sha256("refresh-token")).thenReturn("refresh-hash");
        when(jwtProperties.accessTokenTtlMinutes()).thenReturn(15L);
        when(jwtProperties.refreshTokenTtlDays()).thenReturn(30L);
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        AuthResponse response = authService.login(new LoginRequest("admin", "Admin@123"));

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(900, response.expiresInSeconds());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(userRepository).findRoleCodesByUserId(userId);
        verify(permissionCacheService).getAuthorities(userId);
    }

    @Test
    void loginWithWrongPasswordRejects() {
        AuthUserView authUser = new StubAuthUser(userId, schoolId, "admin",
                "hash", "ACTIVE", "en");
        when(userRepository.findAuthUser("admin")).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("admin", "wrong")));
    }

    @Test
    void loginWithInactiveUserRejects() {
        AuthUserView authUser = new StubAuthUser(userId, schoolId, "admin",
                "hash", "LOCKED", "en");
        when(userRepository.findAuthUser("admin")).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("Admin@123", "hash")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("admin", "Admin@123")));
    }

    @Test
    void loginWithUnknownUserRejects() {
        when(userRepository.findAuthUser("nobody")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("nobody", "Admin@123")));
    }

    record StubAuthUser(UUID id, UUID schoolId, String username, String passwordHash,
                        String status, String locale) implements AuthUserView {
        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public UUID getSchoolId() {
            return schoolId;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getPasswordHash() {
            return passwordHash;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public String getLocale() {
            return locale;
        }

        @Override
        public String getFirstName() {
            return null;
        }

        @Override
        public String getLastName() {
            return null;
        }

        @Override
        public String getEmail() {
            return null;
        }
    }
}
