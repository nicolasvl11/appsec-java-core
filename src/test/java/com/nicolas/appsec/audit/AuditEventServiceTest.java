package com.nicolas.appsec.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    @Mock AuditEventRepository repo;
    @InjectMocks AuditEventService service;

    private final ObjectMapper mapper = new ObjectMapper();

    // ── recordHttpEvent ───────────────────────────────────────────────────────

    @Test
    void recordHttpEvent_saves_event_with_correct_fields() {
        // Re-create service with real ObjectMapper since @InjectMocks only injects mocks
        AuditEventService svc = new AuditEventService(repo, mapper);

        svc.recordHttpEvent("alice", "GET", "/api/v1/ping", 200, "127.0.0.1", "curl/8", 42L, "req-1");

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());

        AuditEvent e = cap.getValue();
        assertThat(e.getActor()).isEqualTo("alice");
        assertThat(e.getAction()).isEqualTo("http_request");
        assertThat(e.getTarget()).isEqualTo("/api/v1/ping");
        assertThat(e.getMeta().get("method").asText()).isEqualTo("GET");
        assertThat(e.getMeta().get("status").asInt()).isEqualTo(200);
        assertThat(e.getMeta().get("ip").asText()).isEqualTo("127.0.0.1");
        assertThat(e.getMeta().get("durationMs").asLong()).isEqualTo(42L);
        assertThat(e.getMeta().get("requestId").asText()).isEqualTo("req-1");
        assertThat(e.getEventTime()).isNotNull();
    }

    @Test
    void recordHttpEvent_uses_anonymous_when_actor_blank() {
        AuditEventService svc = new AuditEventService(repo, mapper);

        svc.recordHttpEvent("", "GET", "/ping", 200, "127.0.0.1", null, 1L, null);

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getActor()).isEqualTo("anonymous");
    }

    @Test
    void recordHttpEvent_handles_null_ip_and_user_agent() {
        AuditEventService svc = new AuditEventService(repo, mapper);

        svc.recordHttpEvent("alice", "GET", "/ping", 200, null, null, 1L, null);

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getMeta().get("ip").asText()).isEqualTo("");
        assertThat(cap.getValue().getMeta().get("userAgent").asText()).isEqualTo("");
    }

    // ── recordSecurityEvent ───────────────────────────────────────────────────

    @Test
    void recordSecurityEvent_saves_event_with_correct_fields() {
        AuditEventService svc = new AuditEventService(repo, mapper);

        svc.recordSecurityEvent("alice", "login_success", "/api/v1/auth/login", Map.of("key", "val"));

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());

        AuditEvent e = cap.getValue();
        assertThat(e.getActor()).isEqualTo("alice");
        assertThat(e.getAction()).isEqualTo("login_success");
        assertThat(e.getTarget()).isEqualTo("/api/v1/auth/login");
        assertThat(e.getMeta().get("key").asText()).isEqualTo("val");
        assertThat(e.getEventTime()).isNotNull();
    }

    @Test
    void recordSecurityEvent_handles_null_meta() {
        AuditEventService svc = new AuditEventService(repo, mapper);

        svc.recordSecurityEvent("alice", "logout", "/api/v1/auth/logout", null);

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getMeta().isEmpty()).isTrue();
    }

    @Test
    void recordSecurityEvent_uses_anonymous_when_actor_null() {
        AuditEventService svc = new AuditEventService(repo, mapper);

        svc.recordSecurityEvent(null, "login_failure", "/api/v1/auth/login", Map.of());

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getActor()).isEqualTo("anonymous");
    }

    // ── semantic flag ─────────────────────────────────────────────────────────

    @Test
    void recordSecurityEvent_sets_semantic_flag_on_request() {
        AuditEventService svc = new AuditEventService(repo, mapper);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            svc.recordSecurityEvent("alice", "login_success", "/api/v1/auth/login", Map.of());
            assertThat(request.getAttribute(AuditEventService.SEMANTIC_EVENT_ATTR)).isEqualTo(Boolean.TRUE);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void recordSecurityEvent_does_not_throw_when_no_request_context() {
        AuditEventService svc = new AuditEventService(repo, mapper);
        RequestContextHolder.resetRequestAttributes();

        // Must not throw even outside a request (e.g. async jobs, tests)
        svc.recordSecurityEvent("alice", "login_success", "/api/v1/auth/login", Map.of());

        verify(repo).save(any());
    }
}
