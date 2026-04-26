package com.nicolas.appsec.ratelimit;

/**
 * Sliding-window rate limiter contract.
 *
 * <p>Current implementation: {@link InMemoryRateLimiter} — suitable for a single-instance
 * deployment. A Redis-backed implementation would look like:
 *
 * <pre>
 * class RedisRateLimiter implements RateLimiter {
 *     // Uses a Lua script executed atomically via EVALSHA for increment + TTL.
 *     // Key: "rl:{path}:{ip}", value: sliding counter, TTL = windowSeconds.
 *     // Guarantees correctness under horizontal scale-out with no local state.
 * }
 * </pre>
 *
 * <p>To swap implementations: replace the {@code @Bean InMemoryRateLimiter} in
 * {@code SecurityConfig} with a {@code RedisRateLimiter} bean — no other code changes needed.
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
