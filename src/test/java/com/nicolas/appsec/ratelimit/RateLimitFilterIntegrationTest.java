package com.nicolas.appsec.ratelimit;

import com.nicolas.appsec.api.PingController;
import com.nicolas.appsec.audit.AuditEventService;
import com.nicolas.appsec.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PingController.class)
@Import(SecurityConfig.class)
class RateLimitFilterIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AuditEventService auditEventService;

    @Test
    void ping_hits_429_after_30_requests_for_same_ip() throws Exception {
        doNothing().when(auditEventService).recordHttpEvent(
                anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), anyLong()
        );

        for (int i = 0; i < 30; i++) {
            mvc.perform(
                    get("/api/v1/ping")
                            .header("X-Forwarded-For", "203.0.113.10")
                            .with(r -> {
                                r.setRemoteAddr("127.0.0.1");
                                return r;
                            })
            )
            .andExpect(status().isOk())
            .andExpect(header().exists("RateLimit-Limit"))
            .andExpect(header().exists("RateLimit-Remaining"))
            .andExpect(header().exists("RateLimit-Reset"));
        }

        mvc.perform(
                get("/api/v1/ping")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .with(r -> {
                            r.setRemoteAddr("127.0.0.1");
                            return r;
                        })
        )
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(header().string("RateLimit-Remaining", "0"));
    }

    @Test
    void xff_is_ignored_when_proxy_is_not_trusted() throws Exception {
        doNothing().when(auditEventService).recordHttpEvent(
                anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), anyLong()
        );

        for (int i = 0; i < 30; i++) {
            mvc.perform(
                    get("/api/v1/ping")
                            .header("X-Forwarded-For", "203.0.113." + i)
                            .with(r -> {
                                r.setRemoteAddr("10.10.10.20");
                                return r;
                            })
            )
            .andExpect(status().isOk());
        }

        mvc.perform(
                get("/api/v1/ping")
                        .header("X-Forwarded-For", "203.0.113.99")
                        .with(r -> {
                            r.setRemoteAddr("10.10.10.20");
                            return r;
                        })
        )
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
    }

    @Test
    void trusted_proxy_uses_xff_and_keeps_counters_separate_per_client_ip() throws Exception {
        doNothing().when(auditEventService).recordHttpEvent(
                anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), anyLong()
        );

        for (int i = 0; i < 30; i++) {
            mvc.perform(
                    get("/api/v1/ping")
                            .header("X-Forwarded-For", "203.0.113.20")
                            .with(r -> {
                                r.setRemoteAddr("127.0.0.1");
                                return r;
                            })
            )
            .andExpect(status().isOk());
        }

        mvc.perform(
                get("/api/v1/ping")
                        .header("X-Forwarded-For", "203.0.113.20")
                        .with(r -> {
                            r.setRemoteAddr("127.0.0.1");
                            return r;
                        })
        )
        .andExpect(status().isTooManyRequests());

        mvc.perform(
                get("/api/v1/ping")
                        .header("X-Forwarded-For", "203.0.113.21")
                        .with(r -> {
                            r.setRemoteAddr("127.0.0.1");
                            return r;
                        })
        )
        .andExpect(status().isOk())
        .andExpect(header().string("RateLimit-Remaining", "29"));
    }

    @Test
    void blocked_response_returns_expected_problem_detail_body() throws Exception {
        doNothing().when(auditEventService).recordHttpEvent(
                anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), anyLong()
        );

        for (int i = 0; i < 30; i++) {
            mvc.perform(
                    get("/api/v1/ping")
                            .header("X-Forwarded-For", "203.0.113.30")
                            .with(r -> {
                                r.setRemoteAddr("127.0.0.1");
                                return r;
                            })
            )
            .andExpect(status().isOk());
        }

        mvc.perform(
                get("/api/v1/ping")
                        .header("X-Forwarded-For", "203.0.113.30")
                        .with(r -> {
                            r.setRemoteAddr("127.0.0.1");
                            return r;
                        })
        )
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(header().string("RateLimit-Remaining", "0"))
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.title").value("Too Many Requests"))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.detail").value("Rate limit exceeded for this client and endpoint."))
        .andExpect(jsonPath("$.retryAfterSeconds").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/ping"));
    }
}