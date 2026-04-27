package com.nicolas.appsec.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtBlacklistServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;

    @Test
    void blacklist_stores_key_with_ttl() {
        when(redis.opsForValue()).thenReturn(ops);
        JwtBlacklistService service = new JwtBlacklistService(redis);

        service.blacklist("test-jti", Instant.now().plusSeconds(300));

        verify(ops).set(eq("jwt:blacklist:test-jti"), eq("1"), any(Duration.class));
    }

    @Test
    void isBlacklisted_returns_true_when_key_exists() {
        when(redis.hasKey("jwt:blacklist:test-jti")).thenReturn(true);
        JwtBlacklistService service = new JwtBlacklistService(redis);

        assertThat(service.isBlacklisted("test-jti")).isTrue();
    }

    @Test
    void isBlacklisted_returns_false_when_key_absent() {
        when(redis.hasKey("jwt:blacklist:missing")).thenReturn(false);
        JwtBlacklistService service = new JwtBlacklistService(redis);

        assertThat(service.isBlacklisted("missing")).isFalse();
    }

    @Test
    void blacklist_fails_open_when_redis_throws() {
        when(redis.opsForValue()).thenThrow(new RuntimeException("Redis down"));
        JwtBlacklistService service = new JwtBlacklistService(redis);

        service.blacklist("jti", Instant.now().plusSeconds(60));
        // no exception propagated — fail-open
    }

    @Test
    void isBlacklisted_fails_open_when_redis_throws() {
        when(redis.hasKey(anyString())).thenThrow(new RuntimeException("Redis down"));
        JwtBlacklistService service = new JwtBlacklistService(redis);

        assertThat(service.isBlacklisted("jti")).isFalse();
    }
}
