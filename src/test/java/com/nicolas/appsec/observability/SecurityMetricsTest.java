package com.nicolas.appsec.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityMetricsTest {

    SecurityMetrics metrics;
    SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new SecurityMetrics(registry);
    }

    @Test
    void http_requests_counter_starts_at_zero() {
        Counter c = registry.find("http.requests.total").counter();
        assertThat(c).isNotNull();
        assertThat(c.count()).isZero();
    }

    @Test
    void increment_http_requests_increases_counter() {
        metrics.incrementHttpRequests();
        metrics.incrementHttpRequests();
        assertThat(registry.find("http.requests.total").counter().count()).isEqualTo(2.0);
    }

    @Test
    void increment_rate_limit_blocked_increases_counter() {
        metrics.incrementRateLimitBlocked();
        assertThat(registry.find("ratelimit.blocked.total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void increment_auth_failures_increases_counter() {
        metrics.incrementAuthFailures();
        metrics.incrementAuthFailures();
        metrics.incrementAuthFailures();
        assertThat(registry.find("auth.failures.total").counter().count()).isEqualTo(3.0);
    }

    @Test
    void all_three_counters_are_independent() {
        metrics.incrementHttpRequests();
        metrics.incrementRateLimitBlocked();
        assertThat(registry.find("http.requests.total").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("ratelimit.blocked.total").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("auth.failures.total").counter().count()).isZero();
    }
}
