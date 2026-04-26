package com.nicolas.appsec.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class SecurityMetrics {

    private final Counter httpRequestsTotal;
    private final Counter rateLimitBlockedTotal;
    private final Counter authFailuresTotal;

    public SecurityMetrics(MeterRegistry registry) {
        this.httpRequestsTotal = Counter.builder("http.requests.total")
                .description("Total number of HTTP requests processed by audit filter")
                .register(registry);

        this.rateLimitBlockedTotal = Counter.builder("ratelimit.blocked.total")
                .description("Total number of requests blocked by the rate limiter")
                .register(registry);

        this.authFailuresTotal = Counter.builder("auth.failures.total")
                .description("Total number of authentication failures (401 responses)")
                .register(registry);
    }

    public void incrementHttpRequests() {
        httpRequestsTotal.increment();
    }

    public void incrementRateLimitBlocked() {
        rateLimitBlockedTotal.increment();
    }

    public void incrementAuthFailures() {
        authFailuresTotal.increment();
    }
}
