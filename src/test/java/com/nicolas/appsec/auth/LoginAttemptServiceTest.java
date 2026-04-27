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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginAttemptServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;

    @Test
    void record_failure_increments_key_and_sets_expiry_on_first_failure() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment("login:fail:alice")).thenReturn(1L);
        LoginAttemptService svc = new LoginAttemptService(redis);

        svc.recordFailure("alice");

        verify(ops).increment("login:fail:alice");
        verify(redis).expire(eq("login:fail:alice"), any(Duration.class));
    }

    @Test
    void record_failure_does_not_reset_expiry_on_subsequent_calls() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment("login:fail:alice")).thenReturn(2L);
        LoginAttemptService svc = new LoginAttemptService(redis);

        svc.recordFailure("alice");

        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void is_locked_returns_false_below_threshold() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("login:fail:alice")).thenReturn("5");
        LoginAttemptService svc = new LoginAttemptService(redis);

        assertThat(svc.isLocked("alice")).isFalse();
    }

    @Test
    void is_locked_returns_true_at_max_attempts() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("login:fail:alice")).thenReturn(String.valueOf(LoginAttemptService.MAX_ATTEMPTS));
        LoginAttemptService svc = new LoginAttemptService(redis);

        assertThat(svc.isLocked("alice")).isTrue();
    }

    @Test
    void is_locked_returns_false_when_key_absent() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("login:fail:alice")).thenReturn(null);
        LoginAttemptService svc = new LoginAttemptService(redis);

        assertThat(svc.isLocked("alice")).isFalse();
    }

    @Test
    void reset_deletes_key() {
        LoginAttemptService svc = new LoginAttemptService(redis);
        svc.reset("alice");
        verify(redis).delete("login:fail:alice");
    }

    @Test
    void get_retry_after_returns_ttl_from_redis() {
        when(redis.getExpire("login:fail:alice", TimeUnit.SECONDS)).thenReturn(300L);
        LoginAttemptService svc = new LoginAttemptService(redis);

        assertThat(svc.getRetryAfterSeconds("alice")).isEqualTo(300L);
    }

    @Test
    void fails_open_when_redis_throws_on_is_locked() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenThrow(new RuntimeException("Redis down"));
        LoginAttemptService svc = new LoginAttemptService(redis);

        assertThat(svc.isLocked("alice")).isFalse();
    }

    @Test
    void fails_open_when_redis_throws_on_record_failure() {
        when(redis.opsForValue()).thenThrow(new RuntimeException("Redis down"));
        LoginAttemptService svc = new LoginAttemptService(redis);

        svc.recordFailure("alice"); // must not throw
    }
}
