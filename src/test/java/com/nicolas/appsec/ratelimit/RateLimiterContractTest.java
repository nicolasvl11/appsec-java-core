package com.nicolas.appsec.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test for {@link RateLimiter}. Any implementation must pass these cases.
 * When a Redis implementation is added, subclass this test and override {@link #limiter()}.
 */
class RateLimiterContractTest {

    RateLimiter limiter() {
        return new InMemoryRateLimiter(Clock.systemUTC());
    }

    @Test
    void first_request_is_allowed() {
        RateLimiter.Decision d = limiter().allow("key1", 5, 60);
        assertThat(d.allowed()).isTrue();
        assertThat(d.remaining()).isEqualTo(4);
        assertThat(d.limit()).isEqualTo(5);
    }

    @Test
    void request_at_limit_boundary_is_allowed() {
        RateLimiter rl = limiter();
        RateLimiter.Decision last = null;
        for (int i = 0; i < 5; i++) {
            last = rl.allow("key2", 5, 60);
        }
        assertThat(last.allowed()).isTrue();
        assertThat(last.remaining()).isZero();
    }

    @Test
    void request_over_limit_is_denied() {
        RateLimiter rl = limiter();
        for (int i = 0; i < 5; i++) rl.allow("key3", 5, 60);
        RateLimiter.Decision d = rl.allow("key3", 5, 60);
        assertThat(d.allowed()).isFalse();
        assertThat(d.remaining()).isZero();
        assertThat(d.retryAfterSeconds()).isPositive();
    }

    @Test
    void different_keys_are_independent() {
        RateLimiter rl = limiter();
        for (int i = 0; i < 5; i++) rl.allow("keyA", 5, 60);
        RateLimiter.Decision d = rl.allow("keyB", 5, 60);
        assertThat(d.allowed()).isTrue();
        assertThat(d.remaining()).isEqualTo(4);
    }

    @Test
    void reset_epoch_is_in_the_future() {
        RateLimiter.Decision d = limiter().allow("key5", 5, 60);
        long now = System.currentTimeMillis() / 1000;
        assertThat(d.resetEpochSeconds()).isGreaterThan(now);
    }
}
