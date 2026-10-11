package com.schoolms.auth;

import com.schoolms.auth.dto.GeneratedCredentials;
import com.schoolms.common.enums.UserStatus;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.security.SecurityUtils;
import com.schoolms.user.Role;
import com.schoolms.user.RoleRepository;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import com.schoolms.user.UserRole;
import com.schoolms.user.UserRoleRepository;
import com.schoolms.user.dto.ProvisionedAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CredentialAccountService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CredentialGenerator credentialGenerator;
    private final PasswordEncoder passwordEncoder;
    private final CredentialAuditRepository credentialAuditRepository;

    @Transactional
    public ProvisionedAccount provision(UUID schoolId, String roleCode, String firstName, String lastName,
                                       String email, String phone) {
        GeneratedCredentials creds = credentialGenerator.generate(schoolId, roleCode, firstName);
        User user = new User();
        user.setSchoolId(schoolId);
        user.setUsername(creds.username());
        user.setEmail(blankToNull(email));
        user.setPhone(blankToNull(phone));
        user.setPasswordHash(passwordEncoder.encode(creds.temporaryPassword()));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLocale("en");
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(true);
        user = userRepository.save(user);
        assignRole(user, roleCode);
        audit(schoolId, currentActorOrNull(), user.getId(), "ACCOUNT_CREATED", user.getUsername());
        return new ProvisionedAccount(user.getId(), creds.username(), creds.temporaryPassword());
    }

    @Transactional
    public void bumpTokenVersionAndRevoke(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    public void audit(UUID schoolId, UUID actorUserId, UUID targetUserId, String action, String detail) {
        CredentialAudit row = new CredentialAudit();
        row.setSchoolId(schoolId);
        row.setActorUserId(actorUserId);
        row.setTargetUserId(targetUserId);
        row.setAction(action);
        row.setDetail(detail);
        credentialAuditRepository.save(row);
    }

    private void assignRole(User user, String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return;
        }
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException("user.role_not_found"));
        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setSchoolId(user.getSchoolId());
        userRoleRepository.save(userRole);
    }

    private UUID currentActorOrNull() {
        try {
            return SecurityUtils.currentUserId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
