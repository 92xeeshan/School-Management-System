package com.schoolms.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves a user's effective authorities (permission codes) from the
 * role_permission matrix, cached in Redis with a short TTL so permission
 * changes take effect without forcing a re-login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private static final String KEY_PREFIX = "schoolms:auth:perms:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public Set<String> getAuthorities(UUID userId) {
        String key = KEY_PREFIX + userId;
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<Set<String>>() {
                });
            } catch (Exception e) {
                log.warn("Failed to parse cached authorities for user {}, refreshing", userId);
            }
        }
        return loadAndCache(key, userId);
    }

    public void evict(UUID userId) {
        redis.delete(KEY_PREFIX + userId);
    }

    private Set<String> loadAndCache(String key, UUID userId) {
        List<String> codes = userRepository.findPermissionCodesByUserId(userId);
        Set<String> authorities = new HashSet<>(codes);
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(authorities), TTL);
        } catch (Exception e) {
            log.warn("Could not cache authorities for user {}", userId);
        }
        return authorities;
    }
}
