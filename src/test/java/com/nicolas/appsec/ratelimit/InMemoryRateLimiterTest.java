package com.nicolas.appsec.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    @Test
    void allows_until_limit_then_blocks_with_retry_after() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);

        int limit = 3;
        int windowSeconds = 60;

        assertThat(limiter.allow("k", limit, windowSeconds).allowed()).isTrue();
        assertThat(limiter.allow("k", limit, windowSeconds).allowed()).isTrue();
        assertThat(limiter.allow("k", limit, windowSeconds).allowed()).isTrue();

        InMemoryRateLimiter.Decision blocked = limiter.allow("k", limit, windowSeconds);

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.limit()).isEqualTo(limit);
        assertThat(blocked.remaining()).isEqualTo(0);
        assertThat(blocked.retryAfterSeconds()).isBetween(1L, 60L);
        assertThat(blocked.resetEpochSeconds()).isGreaterThan(0);
    }

    @Test
    void remaining_decreases_on_each_request() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);

        int limit = 3;
        int windowSeconds = 60;

        InMemoryRateLimiter.Decision d1 = limiter.allow("k", limit, windowSeconds);
        InMemoryRateLimiter.Decision d2 = limiter.allow("k", limit, windowSeconds);

        assertThat(d1.remaining()).isEqualTo(2);
        assertThat(d2.remaining()).isEqualTo(1);
    }
}