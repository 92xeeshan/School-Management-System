package com.schoolms.website;

import com.schoolms.common.exception.RateLimitException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PublicRateLimiter {

    private static final int GET_LIMIT_PER_MINUTE = 60;
    private static final int POST_LIMIT_PER_MINUTE = 5;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, List<Long>> hits = new ConcurrentHashMap<>();

    public void enforceGet(String key) {
        enforce(key + ":get", GET_LIMIT_PER_MINUTE);
    }

    public void enforcePost(String key) {
        enforce(key + ":post", POST_LIMIT_PER_MINUTE);
    }

    private void enforce(String key, int limit) {
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;
        List<Long> timestamps = hits.computeIfAbsent(key, ignored -> new ArrayList<>());
        synchronized (timestamps) {
            timestamps.removeIf(ts -> ts < windowStart);
            if (timestamps.size() >= limit) {
                throw new RateLimitException();
            }
            timestamps.add(now);
        }
    }
}
