package com.nicolas.appsec.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Fixed-window rate limiter backed by Redis using an atomic Lua script.
 * Safe under horizontal scale-out: INCR + EXPIRE are executed in a single
 * round-trip with no TOCTOU gap.
 *
 * Activated via {@code app.ratelimit.type=redis}.
 */
public class RedisRateLimiter implements RateLimiter {

    private static final String LUA = """
            local key    = KEYS[1]
            local limit  = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local now    = tonumber(ARGV[3])
            local count  = redis.call('INCR', key)
            if count == 1 then redis.call('EXPIRE', key, window) end
            local ttl = redis.call('TTL', key)
            if ttl < 0 then ttl = window end
            local reset     = now + ttl
            local remaining = math.max(0, limit - count)
            if count > limit then
                return {0, remaining, reset, ttl}
            else
                return {1, remaining, reset, 0}
            end
            """;

    @SuppressWarnings("unchecked")
    private static final RedisScript<List<Object>> SCRIPT =
            RedisScript.of(LUA, (Class<List<Object>>) (Class<?>) List.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    public RedisRateLimiter(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    @Override
    public Decision allow(String key, int limit, int windowSeconds) {
        long now = Instant.now(clock).getEpochSecond();

        List<Object> result = redis.execute(SCRIPT,
                List.of("rl:" + key),
                String.valueOf(limit),
                String.valueOf(windowSeconds),
                String.valueOf(now));

        if (result == null || result.size() < 4) {
            // Fail open: keep service available even if Redis is unreachable.
            return new Decision(true, 0, limit, limit, now + windowSeconds);
        }

        boolean allowed  = toLong(result.get(0)) == 1L;
        int remaining    = (int) toLong(result.get(1));
        long resetEpoch  = toLong(result.get(2));
        long retryAfter  = toLong(result.get(3));

        return new Decision(allowed, retryAfter, limit, remaining, resetEpoch);
    }

    private static long toLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }
}
