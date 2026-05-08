package com.nicolas.appsec.audit;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "RUN_TC", matches = "true")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuditLoggingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired AuditEventRepository repo;

    @Test
    void ping_creates_audit_event() throws Exception {
        long before = repo.count();

        mvc.perform(get("/api/v1/ping"))
           .andExpect(status().isOk());

        long after = repo.count();
        assertThat(after).isGreaterThan(before);
    }

    @Test
    void audit_event_meta_contains_request_id() throws Exception {
        String clientRequestId = "test-req-" + System.currentTimeMillis();

        mvc.perform(get("/api/v1/ping").header("X-Request-Id", clientRequestId))
           .andExpect(status().isOk());

        List<AuditEvent> events = repo.findAll();
        AuditEvent latest = events.get(events.size() - 1);

        assertThat(latest.getMeta()).isNotNull();
        assertThat(latest.getMeta().get("requestId").asText()).isEqualTo(clientRequestId);
    }

    @Test
    void audit_event_meta_contains_method_and_status() throws Exception {
        mvc.perform(get("/api/v1/ping"))
           .andExpect(status().isOk());

        List<AuditEvent> events = repo.findAll();
        AuditEvent latest = events.get(events.size() - 1);

        assertThat(latest.getMeta().get("method").asText()).isEqualTo("GET");
        assertThat(latest.getMeta().get("status").asInt()).isEqualTo(200);
        assertThat(latest.getMeta().get("requestId").asText()).isNotBlank();
    }

    @Test
    void successful_login_produces_exactly_one_audit_event() throws Exception {
        // Register user first
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"dedup_user\",\"password\":\"Pass1234!\"}"))
           .andExpect(status().isCreated());

        java.time.Instant beforeLogin = java.time.Instant.now();

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"dedup_user\",\"password\":\"Pass1234!\"}"))
           .andExpect(status().isOk());

        List<AuditEvent> loginEvents = repo.findAll().stream()
                .filter(e -> e.getEventTime().isAfter(beforeLogin))
                .filter(e -> "dedup_user".equals(e.getActor()))
                .toList();

        // Exactly one event: login_success (not http_request duplicate)
        assertThat(loginEvents).hasSize(1);
        assertThat(loginEvents.get(0).getAction()).isEqualTo("login_success");
    }

    @Test
    void failed_login_produces_exactly_one_audit_event() throws Exception {
        long before = repo.count();

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"no_such_user\",\"password\":\"wrong\"}"))
           .andExpect(status().isUnauthorized());

        List<AuditEvent> newEvents = repo.findAll().stream()
                .filter(e -> e.getEventTime().isAfter(java.time.Instant.now().minusSeconds(5)))
                .filter(e -> "no_such_user".equals(e.getActor()))
                .toList();

        assertThat(newEvents).hasSize(1);
        assertThat(newEvents.get(0).getAction()).isEqualTo("login_failure");
    }
}
