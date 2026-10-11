package com.schoolms.auth;

import com.schoolms.auth.dto.AuthResponse;
import com.schoolms.auth.dto.ChangePasswordRequest;
import com.schoolms.auth.dto.FirstLoginSetupRequest;
import com.schoolms.auth.dto.ForgotPasswordCompleteRequest;
import com.schoolms.auth.dto.ForgotPasswordStartRequest;
import com.schoolms.auth.dto.ForgotPasswordStartResponse;
import com.schoolms.auth.dto.ForgotPasswordVerifyRequest;
import com.schoolms.auth.dto.ForgotPasswordVerifyResponse;
import com.schoolms.auth.dto.LinkedChildDto;
import com.schoolms.auth.dto.LoginRequest;
import com.schoolms.auth.dto.RefreshRequest;
import com.schoolms.auth.dto.SecurityQuestionDto;
import com.schoolms.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final CredentialService credentialService;

    @Operation(summary = "Sign in and receive JWT access + refresh tokens")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "Rotate a refresh token for a new token pair")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @Operation(summary = "Revoke the current refresh token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestParam String refreshToken) {
        authService.logout(refreshToken);
        return ApiResponse.okMessage("auth.logout_success");
    }

    @Operation(summary = "Current authenticated user profile")
    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.ok(authService.me());
    }

    @Operation(summary = "Catalog of security questions")
    @GetMapping("/questions")
    public ApiResponse<List<SecurityQuestionDto>> questions() {
        return ApiResponse.ok(credentialService.listQuestions());
    }

    @Operation(summary = "Complete first-login password and security questions")
    @PostMapping("/first-login")
    public ApiResponse<Void> firstLogin(@Valid @RequestBody FirstLoginSetupRequest request) {
        credentialService.completeFirstLogin(request);
        return ApiResponse.okMessage("auth.first_login_complete");
    }

    @Operation(summary = "Change password from the profile menu")
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        credentialService.changePassword(request);
        return ApiResponse.okMessage("auth.password_changed");
    }

    @Operation(summary = "Start forgot-password by username")
    @PostMapping("/forgot")
    public ApiResponse<ForgotPasswordStartResponse> forgot(@Valid @RequestBody ForgotPasswordStartRequest request) {
        return ApiResponse.ok(credentialService.startForgot(request));
    }

    @Operation(summary = "Verify security-question answers for password reset")
    @PostMapping("/forgot/verify")
    public ApiResponse<ForgotPasswordVerifyResponse> verifyForgot(
            @Valid @RequestBody ForgotPasswordVerifyRequest request) {
        return ApiResponse.ok(credentialService.verifyForgot(request));
    }

    @Operation(summary = "Set a new password after verifying security questions")
    @PostMapping("/forgot/complete")
    public ApiResponse<Void> completeForgot(@Valid @RequestBody ForgotPasswordCompleteRequest request) {
        credentialService.completeForgot(request);
        return ApiResponse.okMessage("auth.password_reset");
    }

    @Operation(summary = "Children linked to the current parent login")
    @GetMapping("/children")
    public ApiResponse<List<LinkedChildDto>> children() {
        return ApiResponse.ok(authService.myChildren());
    }
}
