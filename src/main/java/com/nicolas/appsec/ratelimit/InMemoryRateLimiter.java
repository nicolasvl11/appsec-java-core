package com.nicolas.appsec.ratelimit;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimiter {

    public record Decision(
            boolean allowed,
            long retryAfterSeconds,
            int limit,
            int remaining,
            long resetEpochSeconds
    ) {}

    private record Window(long windowStartEpochSec, int count) {}

    private final Map<String, Window> store = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public Decision allow(String key, int limit, int windowSeconds) {
        long now = Instant.now(clock).getEpochSecond();
        long windowStart = now - (now % windowSeconds);
        long reset = windowStart + windowSeconds;

        Window updated = store.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStartEpochSec != windowStart) {
                return new Window(windowStart, 1);
            }
            return new Window(existing.windowStartEpochSec, existing.count + 1);
        });

        int used = updated.count;
        int remaining = Math.max(0, limit - used);

        if (used <= limit) {
            return new Decision(true, 0, limit, remaining, reset);
        }

        long retryAfter = Math.max(1, reset - now);
        return new Decision(false, retryAfter, limit, 0, reset);
    }
}