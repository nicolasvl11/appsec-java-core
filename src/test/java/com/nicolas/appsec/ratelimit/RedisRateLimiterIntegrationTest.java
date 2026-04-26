package com.nicolas.appsec.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "app.ratelimit.type=redis",
        "management.health.redis.enabled=true"
})
@Testcontainers(disabledWithoutDocker = true)
class RedisRateLimiterIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    RateLimiter rateLimiter;

    @Test
    void rateLimiter_is_redis_implementation() {
        assertThat(rateLimiter).isInstanceOf(RedisRateLimiter.class);
    }

    @Test
    void allows_requests_within_limit() {
        RateLimiter.Decision d = rateLimiter.allow("it-allow-" + System.nanoTime(), 5, 60);

        assertThat(d.allowed()).isTrue();
        assertThat(d.remaining()).isEqualTo(4);
        assertThat(d.limit()).isEqualTo(5);
    }

    @Test
    void denies_request_that_exceeds_limit() {
        String key = "it-deny-" + System.nanoTime();
        for (int i = 0; i < 5; i++) {
            rateLimiter.allow(key, 5, 60);
        }

        RateLimiter.Decision denied = rateLimiter.allow(key, 5, 60);

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.remaining()).isZero();
        assertThat(denied.retryAfterSeconds()).isPositive();
    }

    @Test
    void remaining_decrements_on_each_request() {
        String key = "it-decrement-" + System.nanoTime();
        int limit = 10;

        for (int i = 1; i <= 3; i++) {
            RateLimiter.Decision d = rateLimiter.allow(key, limit, 60);
            assertThat(d.allowed()).isTrue();
            assertThat(d.remaining()).isEqualTo(limit - i);
        }
    }
}
