package com.schoolms.auth;

import com.schoolms.auth.dto.ChangePasswordRequest;
import com.schoolms.auth.dto.CredentialAuditDto;
import com.schoolms.auth.dto.FirstLoginSetupRequest;
import com.schoolms.auth.dto.ForgotPasswordCompleteRequest;
import com.schoolms.auth.dto.ForgotPasswordStartRequest;
import com.schoolms.auth.dto.ForgotPasswordStartResponse;
import com.schoolms.auth.dto.ForgotPasswordVerifyRequest;
import com.schoolms.auth.dto.ForgotPasswordVerifyResponse;
import com.schoolms.auth.dto.GeneratedCredentials;
import com.schoolms.auth.dto.ResetPasswordRequest;
import com.schoolms.auth.dto.SecurityAnswerInput;
import com.schoolms.auth.dto.SecurityQuestionDto;
import com.schoolms.common.enums.UserStatus;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.security.JwtService;
import com.schoolms.security.SecurityUtils;
import com.schoolms.tenant.TenantBinder;
import com.schoolms.tenant.TenantContext;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import com.schoolms.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CredentialService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCKOUT = Duration.ofMinutes(15);
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final SecurityQuestionRepository securityQuestionRepository;
    private final UserSecurityAnswerRepository userSecurityAnswerRepository;
    private final PasswordResetChallengeRepository passwordResetChallengeRepository;
    private final CredentialAuditRepository credentialAuditRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CredentialGenerator credentialGenerator;
    private final CredentialAccountService credentialAccountService;
    private final TenantBinder tenantBinder;

    @Transactional(readOnly = true)
    public List<SecurityQuestionDto> listQuestions() {
        return securityQuestionRepository.findByStatusOrderBySortOrderAsc("ACTIVE").stream()
                .map(q -> new SecurityQuestionDto(q.getId(), q.getCode()))
                .toList();
    }

    @Transactional
    public void completeFirstLogin(FirstLoginSetupRequest request) {
        User user = currentUser();
        if (!user.isMustChangePassword()) {
            throw new BusinessException("auth.first_login_not_required");
        }
        requireMatchingPasswords(request.newPassword(), request.confirmPassword());
        PasswordPolicy.validate(request.newPassword());
        saveAnswers(user, request.answers());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setQuestionsSet(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        credentialAccountService.bumpTokenVersionAndRevoke(user);
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());
        credentialAccountService.audit(user.getSchoolId(), user.getId(), user.getId(),
                "FIRST_LOGIN_SETUP", null);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw AuthException.badCredentials();
        }
        requireMatchingPasswords(request.newPassword(), request.confirmPassword());
        PasswordPolicy.validate(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        credentialAccountService.bumpTokenVersionAndRevoke(user);
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());
        credentialAccountService.audit(user.getSchoolId(), user.getId(), user.getId(),
                "PASSWORD_CHANGED", null);
    }

    @Transactional
    public ForgotPasswordStartResponse startForgot(ForgotPasswordStartRequest request) {
        try {
            tenantBinder.bindBypass();
            User user = userRepository.findAuthUser(request.username())
                    .flatMap(view -> {
                        tenantBinder.bind(view.getSchoolId());
                        return userRepository.findById(view.getId());
                    })
                    .orElse(null);
            if (user == null || !user.isQuestionsSet() || user.getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException("auth.forgot_unavailable");
            }
            List<UserSecurityAnswer> answers = userSecurityAnswerRepository.findByUserId(user.getId());
            if (answers.size() < 2) {
                throw new BusinessException("auth.forgot_unavailable");
            }
            PasswordResetChallenge challenge = new PasswordResetChallenge();
            challenge.setSchoolId(user.getSchoolId());
            challenge.setUserId(user.getId());
            challenge.setTokenHash(jwtService.sha256(jwtService.generateRefreshToken()));
            challenge.setExpiresAt(Instant.now().plus(CHALLENGE_TTL));
            challenge = passwordResetChallengeRepository.save(challenge);
            List<SecurityQuestionDto> questions = answers.stream()
                    .map(a -> new SecurityQuestionDto(a.getQuestion().getId(), a.getQuestion().getCode()))
                    .toList();
            credentialAccountService.audit(user.getSchoolId(), null, user.getId(),
                    "FORGOT_PASSWORD_STARTED", null);
            return new ForgotPasswordStartResponse(challenge.getId(), questions);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public ForgotPasswordVerifyResponse verifyForgot(ForgotPasswordVerifyRequest request) {
        try {
            tenantBinder.bindBypass();
            PasswordResetChallenge challenge = passwordResetChallengeRepository
                    .findByIdAndConsumedAtIsNull(request.challengeId())
                    .orElseThrow(() -> new BusinessException("auth.forgot_invalid"));
            if (challenge.getExpiresAt().isBefore(Instant.now())) {
                throw new BusinessException("auth.forgot_expired");
            }
            User user = userRepository.findById(challenge.getUserId())
                    .orElseThrow(() -> new BusinessException("auth.forgot_invalid"));
            if (!answersMatch(user.getId(), request.answers())) {
                credentialAccountService.audit(user.getSchoolId(), null, user.getId(),
                        "FORGOT_PASSWORD_FAILED", null);
                throw new BusinessException("auth.forgot_answers_mismatch");
            }
            String resetToken = jwtService.generateRefreshToken();
            challenge.setTokenHash(jwtService.sha256(resetToken));
            passwordResetChallengeRepository.save(challenge);
            return new ForgotPasswordVerifyResponse(resetToken);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void completeForgot(ForgotPasswordCompleteRequest request) {
        try {
            tenantBinder.bindBypass();
            requireMatchingPasswords(request.newPassword(), request.confirmPassword());
            PasswordPolicy.validate(request.newPassword());
            PasswordResetChallenge challenge = passwordResetChallengeRepository
                    .findByTokenHash(jwtService.sha256(request.resetToken()))
                    .orElseThrow(() -> new BusinessException("auth.forgot_invalid"));
            if (challenge.getConsumedAt() != null) {
                throw new BusinessException("auth.forgot_invalid");
            }
            if (challenge.getExpiresAt().isBefore(Instant.now())) {
                throw new BusinessException("auth.forgot_expired");
            }
            User user = userRepository.findById(challenge.getUserId())
                    .orElseThrow(() -> new BusinessException("auth.forgot_invalid"));
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            user.setMustChangePassword(false);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            credentialAccountService.bumpTokenVersionAndRevoke(user);
            refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());
            challenge.setConsumedAt(Instant.now());
            passwordResetChallengeRepository.save(challenge);
            credentialAccountService.audit(user.getSchoolId(), user.getId(), user.getId(),
                    "FORGOT_PASSWORD_COMPLETED", null);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public UserDto adminResetPassword(UUID userId, ResetPasswordRequest request) {
        User user = userRepository.findByIdAndSchoolId(userId, SecurityUtils.currentSchoolId())
                .orElseThrow(() -> new BusinessException("user.not_found"));
        String temporary = request.temporaryPassword() == null || request.temporaryPassword().isBlank()
                ? credentialGenerator.temporaryPassword(user.getFirstName())
                : request.temporaryPassword();
        if (request.temporaryPassword() != null && !request.temporaryPassword().isBlank()) {
            PasswordPolicy.validate(temporary);
        }
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        credentialAccountService.bumpTokenVersionAndRevoke(user);
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());
        credentialAccountService.audit(user.getSchoolId(), SecurityUtils.currentUserId(), user.getId(),
                "ADMIN_PASSWORD_RESET", user.getUsername());
        return UserDto.from(user, userRepository.findRoleCodesByUserId(user.getId()), temporary);
    }

    @Transactional(readOnly = true)
    public List<CredentialAuditDto> listAudit(UUID targetUserId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<CredentialAudit> rows = targetUserId == null
                ? credentialAuditRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId)
                : credentialAuditRepository.findBySchoolIdAndTargetUserIdOrderByCreatedAtDesc(schoolId, targetUserId);
        return rows.stream()
                .map(row -> new CredentialAuditDto(
                        row.getId(), row.getActorUserId(), row.getTargetUserId(),
                        row.getAction(), row.getDetail(), row.getCreatedAt()))
                .toList();
    }

    public GeneratedCredentials peekGenerated(String roleCode, String firstName) {
        return credentialGenerator.generate(SecurityUtils.currentSchoolId(), roleCode, firstName);
    }

    private void saveAnswers(User user, List<SecurityAnswerInput> answers) {
        if (answers == null || answers.size() < 2 || answers.size() > 3) {
            throw new BusinessException("auth.questions_count");
        }
        Set<UUID> unique = new HashSet<>();
        Map<UUID, SecurityQuestion> catalog = securityQuestionRepository.findByStatusOrderBySortOrderAsc("ACTIVE")
                .stream()
                .collect(Collectors.toMap(SecurityQuestion::getId, Function.identity()));
        for (SecurityAnswerInput input : answers) {
            if (!unique.add(input.questionId())) {
                throw new BusinessException("auth.questions_duplicate");
            }
            SecurityQuestion question = catalog.get(input.questionId());
            if (question == null) {
                throw new BusinessException("auth.question_not_found");
            }
            if (PasswordPolicy.normalizeAnswer(input.answer()).length() < 2) {
                throw new BusinessException("auth.answer_too_short");
            }
        }
        userSecurityAnswerRepository.deleteByUserId(user.getId());
        userSecurityAnswerRepository.flush();
        for (SecurityAnswerInput input : answers) {
            UserSecurityAnswer row = new UserSecurityAnswer();
            row.setSchoolId(user.getSchoolId());
            row.setUserId(user.getId());
            row.setQuestion(catalog.get(input.questionId()));
            row.setAnswerHash(passwordEncoder.encode(PasswordPolicy.normalizeAnswer(input.answer())));
            userSecurityAnswerRepository.save(row);
        }
    }

    private boolean answersMatch(UUID userId, List<SecurityAnswerInput> submitted) {
        List<UserSecurityAnswer> stored = userSecurityAnswerRepository.findByUserId(userId);
        if (stored.isEmpty() || submitted == null || submitted.size() != stored.size()) {
            return false;
        }
        Map<UUID, UserSecurityAnswer> byQuestion = stored.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity()));
        for (SecurityAnswerInput input : submitted) {
            UserSecurityAnswer row = byQuestion.get(input.questionId());
            if (row == null || !passwordEncoder.matches(
                    PasswordPolicy.normalizeAnswer(input.answer()), row.getAnswerHash())) {
                return false;
            }
        }
        return true;
    }

    private void requireMatchingPasswords(String password, String confirm) {
        if (password == null || !password.equals(confirm)) {
            throw new BusinessException("auth.password_mismatch");
        }
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BusinessException("user.not_found"));
    }
}
