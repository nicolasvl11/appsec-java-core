package com.nicolas.appsec.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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

    @Test
    void resets_counter_when_window_changes() {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000), ZoneOffset.UTC);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);

        int limit = 2;
        int windowSeconds = 60;

        assertThat(limiter.allow("k", limit, windowSeconds).allowed()).isTrue();
        assertThat(limiter.allow("k", limit, windowSeconds).allowed()).isTrue();
        assertThat(limiter.allow("k", limit, windowSeconds).allowed()).isFalse();

        clock.setInstant(Instant.ofEpochSecond(1020 + 40));

        InMemoryRateLimiter.Decision afterReset = limiter.allow("k", limit, windowSeconds);

        assertThat(afterReset.allowed()).isTrue();
        assertThat(afterReset.remaining()).isEqualTo(1);
    }

    @Test
    void different_keys_have_independent_counters() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);

        int limit = 2;
        int windowSeconds = 60;

        assertThat(limiter.allow("key-a", limit, windowSeconds).allowed()).isTrue();
        assertThat(limiter.allow("key-a", limit, windowSeconds).allowed()).isTrue();
        assertThat(limiter.allow("key-a", limit, windowSeconds).allowed()).isFalse();

        InMemoryRateLimiter.Decision keyB = limiter.allow("key-b", limit, windowSeconds);

        assertThat(keyB.allowed()).isTrue();
        assertThat(keyB.remaining()).isEqualTo(1);
    }

    @Test
    void reset_epoch_seconds_is_stable_within_same_window() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);

        int limit = 5;
        int windowSeconds = 60;

        InMemoryRateLimiter.Decision d1 = limiter.allow("k", limit, windowSeconds);
        InMemoryRateLimiter.Decision d2 = limiter.allow("k", limit, windowSeconds);

        assertThat(d1.resetEpochSeconds()).isEqualTo(d2.resetEpochSeconds());
    }

    static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}