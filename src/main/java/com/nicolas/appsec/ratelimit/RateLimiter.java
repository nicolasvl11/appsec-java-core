package com.nicolas.appsec.ratelimit;

/**
 * Fixed-window rate limiter contract.
 *
 * <p>Two implementations: {@link InMemoryRateLimiter} for single-instance deployments and
 * {@link RedisRateLimiter} for horizontal scale-out (atomic Lua script, no TOCTOU gap).
 * Selected at runtime via {@code app.ratelimit.type} ({@code memory} or {@code redis}).
 */
public interface RateLimiter {

    /**
     * Records a request attempt for the given key and returns the rate-limit decision.
     *
     * @param key           uniquely identifies the subject (e.g. {@code "/api/v1/ping|203.0.113.1"})
     * @param limit         maximum number of requests allowed within the window
     * @param windowSeconds length of the fixed window in seconds
     * @return a {@link Decision} with allow/deny status and headers info
     */
    Decision allow(String key, int limit, int windowSeconds);

    record Decision(
            boolean allowed,
            long retryAfterSeconds,
            int limit,
            int remaining,
            long resetEpochSeconds
    ) {}
}
