package com.nicolas.appsec.audit;

import com.nicolas.appsec.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuditEventController.class)
@Import(SecurityConfig.class)
class AuditEventControllerTest {

    @Autowired MockMvc mvc;

    @MockBean AuditEventRepository repo;
    @MockBean AuditEventService auditEventService;

    private AuditEvent sampleEvent() {
        AuditEvent e = new AuditEvent();
        e.setEventTime(Instant.now());
        e.setActor("alice");
        e.setAction("http_request");
        e.setTarget("/api/v1/ping");
        return e;
    }

    @Test
    void unauthenticated_returns_401() throws Exception {
        mvc.perform(get("/api/v1/audit-events/recent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void returns_page_of_events_with_defaults() throws Exception {
        doNothing().when(auditEventService).recordHttpEvent(
                anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), anyLong(), anyString());
        AuditEvent e = sampleEvent();
        when(repo.findAllByOrderByEventTimeDesc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(e), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/v1/audit-events/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].actor").value("alice"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @WithMockUser
    void custom_page_and_size_are_forwarded_to_repo() throws Exception {
        doNothing().when(auditEventService).recordHttpEvent(
                anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), anyLong(), anyString());
        when(repo.findAllByOrderByEventTimeDesc(PageRequest.of(1, 10)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mvc.perform(get("/api/v1/audit-events/recent?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser
    void size_over_100_returns_400() throws Exception {
        mvc.perform(get("/api/v1/audit-events/recent?size=200"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void negative_page_returns_400() throws Exception {
        mvc.perform(get("/api/v1/audit-events/recent?page=-1"))
                .andExpect(status().isBadRequest());
    }
}
