package com.schoolms.website;

import com.schoolms.common.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicRateLimiterTest {

    @Test
    void postLimitThrowsAfterFive() {
        PublicRateLimiter limiter = new PublicRateLimiter();
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.enforcePost("DEMO:127.0.0.1"));
        }
        assertThrows(RateLimitException.class, () -> limiter.enforcePost("DEMO:127.0.0.1"));
    }
}
