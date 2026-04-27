package com.nicolas.appsec.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final String PREFIX = "login:fail:";
    static final int MAX_ATTEMPTS = 10;
    static final int LOCK_SECONDS = 15 * 60; // 15 minutes

    private final StringRedisTemplate redis;

    public LoginAttemptService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void recordFailure(String username) {
        try {
            String key = PREFIX + username.toLowerCase();
            Long count = redis.opsForValue().increment(key);
            if (Long.valueOf(1L).equals(count)) {
                redis.expire(key, Duration.ofSeconds(LOCK_SECONDS));
            }
        } catch (Exception e) {
            log.warn("Redis unavailable — login failure not recorded for user={}", username);
        }
    }

    public boolean isLocked(String username) {
        try {
            String value = redis.opsForValue().get(PREFIX + username.toLowerCase());
            return value != null && Integer.parseInt(value) >= MAX_ATTEMPTS;
        } catch (Exception e) {
            log.warn("Redis unavailable — lockout check skipped for user={}", username);
            return false;
        }
    }

    public long getRetryAfterSeconds(String username) {
        try {
            Long ttl = redis.getExpire(PREFIX + username.toLowerCase(), TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : LOCK_SECONDS;
        } catch (Exception e) {
            return LOCK_SECONDS;
        }
    }

    public void reset(String username) {
        try {
            redis.delete(PREFIX + username.toLowerCase());
        } catch (Exception e) {
            log.warn("Redis unavailable — login counter not reset for user={}", username);
        }
    }
}
