package com.nicolas.appsec.api;

import com.nicolas.appsec.audit.AuditEventService;
import com.nicolas.appsec.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired MockMvc mvc;
    @MockBean AuditEventService auditEventService;

    @Test
    void unauthenticated_returns_401() throws Exception {
        mvc.perform(get("/api/v1/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_role_is_forbidden_403() throws Exception {
        doNothing().when(auditEventService).recordHttpEvent(
                anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), anyLong(), anyString());

        mvc.perform(get("/api/v1/admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_role_returns_200() throws Exception {
        doNothing().when(auditEventService).recordHttpEvent(
                anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), anyLong(), anyString());

        mvc.perform(get("/api/v1/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.area").value("admin"));
    }
}
