package com.nicolas.appsec.security;

import com.nicolas.appsec.api.AdminController;
import com.nicolas.appsec.audit.AuditEventService;
import com.nicolas.appsec.auth.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
class AccessDeniedProblemDetailIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockBean AuditEventService auditEventService;
    @MockBean AdminService adminService;

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void admin_endpoint_with_authenticated_non_admin_user_returns_403_problem_detail() throws Exception {
        mvc.perform(get("/api/v1/admin"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource."))
                .andExpect(jsonPath("$.path").value("/api/v1/admin"));
    }
}