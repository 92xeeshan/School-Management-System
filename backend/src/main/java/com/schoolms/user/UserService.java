package com.schoolms.user;

import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.common.enums.UserStatus;
import com.schoolms.security.PermissionCacheService;
import com.schoolms.security.SecurityUtils;
import com.schoolms.user.dto.CreateUserRequest;
import com.schoolms.user.dto.LocaleRequest;
import com.schoolms.user.dto.RoleRequest;
import com.schoolms.user.dto.UpdateUserRequest;
import com.schoolms.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionCacheService permissionCacheService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        return userRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream()
                .map(user -> UserDto.from(user, rolesOf(user.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto get(UUID id) {
        User user = findInSchool(id);
        return UserDto.from(user, rolesOf(id));
    }

    @Transactional
    public UserDto create(CreateUserRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (userRepository.existsByUsernameAndSchoolId(request.username(), schoolId)) {
            throw new BusinessException("user.username_exists");
        }
        if (request.email() != null && !request.email().isBlank()
                && userRepository.existsByEmailAndSchoolId(request.email(), schoolId)) {
            throw new BusinessException("user.email_exists");
        }

        User user = new User();
        user.setSchoolId(schoolId);
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setLocale(validLocale(request.locale()));
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        if (request.roleCodes() != null) {
            for (String roleCode : request.roleCodes()) {
                assignRoleInternal(user, roleCode);
            }
        }
        return UserDto.from(user, rolesOf(user.getId()));
    }

    @Transactional
    public UserDto update(UUID id, UpdateUserRequest request) {
        User user = findInSchool(id);
        if (request.email() != null && !request.email().isBlank()
                && !request.email().equals(user.getEmail())
                && userRepository.existsByEmailAndSchoolId(request.email(), user.getSchoolId())) {
            throw new BusinessException("user.email_exists");
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.locale() != null) {
            user.setLocale(validLocale(request.locale()));
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        userRepository.save(user);
        permissionCacheService.evict(id);
        return UserDto.from(user, rolesOf(id));
    }

    @Transactional
    public UserDto assignRole(UUID userId, RoleRequest request) {
        User user = findInSchool(userId);
        assignRoleInternal(user, request.roleCode());
        permissionCacheService.evict(userId);
        return UserDto.from(user, rolesOf(userId));
    }

    @Transactional
    public UserDto removeRole(UUID userId, RoleRequest request) {
        User user = findInSchool(userId);
        Role role = roleRepository.findByCode(request.roleCode())
                .orElseThrow(() -> new BusinessException("user.role_not_found"));
        if (role.isSystem() && userRoleRepository.findByUserIdAndRoleId(userId, role.getId()).isPresent()) {
            long roleCount = userRoleRepository.countByRoleId(role.getId());
            if (roleCount <= 1) {
                throw new BusinessException("user.role_not_found");
            }
        }
        userRoleRepository.findByUserIdAndRoleId(userId, role.getId())
                .ifPresent(userRoleRepository::delete);
        permissionCacheService.evict(userId);
        return UserDto.from(user, rolesOf(userId));
    }

    @Transactional
    public void updateMyLocale(LocaleRequest request) {
        UUID userId = SecurityUtils.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("user.not_found"));
        user.setLocale(validLocale(request.locale()));
        userRepository.save(user);
        permissionCacheService.evict(userId);
    }

    private void assignRoleInternal(User user, String roleCode) {
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException("user.role_not_found"));
        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            throw new BusinessException("user.role_already");
        }
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setSchoolId(user.getSchoolId());
        userRoleRepository.save(userRole);
    }

    private User findInSchool(UUID id) {
        return userRepository.findByIdAndSchoolId(id, SecurityUtils.currentSchoolId())
                .orElseThrow(() -> new BusinessException("user.not_found"));
    }

    private List<String> rolesOf(UUID userId) {
        return userRepository.findRoleCodesByUserId(userId);
    }

    private String validLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "en";
        }
        String tag = Locale.forLanguageTag(locale).getLanguage();
        return tag.isBlank() ? "en" : tag;
    }
}
