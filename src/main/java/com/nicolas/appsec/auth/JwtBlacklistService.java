package com.nicolas.appsec.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(JwtBlacklistService.class);
    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redis;

    public JwtBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void blacklist(String jti, Instant expiry) {
        try {
            long ttlSeconds = Math.max(1, expiry.getEpochSecond() - Instant.now().getEpochSecond());
            redis.opsForValue().set(PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Redis unavailable — token jti={} could not be blacklisted", jti);
        }
    }

    public boolean isBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
        } catch (Exception e) {
            log.warn("Redis unavailable — blacklist check skipped for jti={}", jti);
            return false;
        }
    }
}
