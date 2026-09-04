package com.schoolms.security;

import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Resolves a user's effective authorities (permission codes) from the
 * role_permission matrix, cached in Caffeine with a short TTL so permission
 * changes take effect without forcing a re-login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final UserRepository userRepository;

    @Cacheable(cacheNames = "authorities", key = "#userId")
    public Set<String> getAuthorities(UUID userId) {
        return Set.copyOf(userRepository.findPermissionCodesByUserId(userId));
    }

    @CacheEvict(cacheNames = "authorities", key = "#userId")
    public void evict(UUID userId) {
        log.debug("Evicted permission cache for user {}", userId);
    }
}
