package com.nicolas.appsec.security;

import com.nicolas.appsec.audit.AuditEventController;
import com.nicolas.appsec.audit.AuditEventRepository;
import com.nicolas.appsec.audit.AuditEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditEventController.class)
@Import(SecurityConfig.class)
class SecurityProblemDetailIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AuditEventService auditEventService;

    @MockBean
    AuditEventRepository auditEventRepository;

    @Test
    void protected_endpoint_without_credentials_returns_401_problem_detail() throws Exception {
        mvc.perform(get("/api/v1/audit-events/recent"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication is required to access this resource."))
                .andExpect(jsonPath("$.path").value("/api/v1/audit-events/recent"));
    }
}