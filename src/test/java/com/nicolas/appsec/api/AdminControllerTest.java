package com.nicolas.appsec.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolas.appsec.audit.AuditEventService;
import com.nicolas.appsec.auth.AdminService;
import com.nicolas.appsec.auth.Role;
import com.nicolas.appsec.auth.UpdateRoleRequest;
import com.nicolas.appsec.auth.UpdateStatusRequest;
import com.nicolas.appsec.auth.UserSummary;
import com.nicolas.appsec.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuditEventService auditEventService;
    @MockBean AdminService adminService;

    private UserSummary sample(long id, String username, String role) {
        return new UserSummary(id, username, role, Instant.now(), null, null, true, null);
    }

    private UserSummary sampleDisabled(long id, String username) {
        return new UserSummary(id, username, "USER", Instant.now(), null, null, false, null);
    }

    // ── GET /api/v1/admin ────────────────────────────────────────────────────

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

    // ── GET /api/v1/admin/users ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_users_returns_page() throws Exception {
        var page = new PageImpl<>(List.of(sample(1L, "alice", "USER")), PageRequest.of(0, 20), 1);
        when(adminService.listUsers(0, 20)).thenReturn(PageResponse.from(page));

        mvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("alice"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void list_users_forbidden_for_user_role() throws Exception {
        mvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /api/v1/admin/users/{id}/role ─────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_role_returns_updated_user() throws Exception {
        when(adminService.updateRole(eq(2L), eq(Role.ADMIN), eq("admin")))
                .thenReturn(sample(2L, "bob", "ADMIN"));

        mvc.perform(patch("/api/v1/admin/users/2/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoleRequest(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_role_forbidden_for_user_role() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/2/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoleRequest(Role.ADMIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_role_returns_409_when_changing_own_role() throws Exception {
        when(adminService.updateRole(eq(1L), eq(Role.USER), eq("admin")))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change your own role."));

        mvc.perform(patch("/api/v1/admin/users/1/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateRoleRequest(Role.USER))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_user_returns_409_when_deleting_self() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete your own account."))
                .when(adminService).deleteUser(eq(1L), eq("admin"));

        mvc.perform(delete("/api/v1/admin/users/1"))
                .andExpect(status().isConflict());
    }

    // ── PATCH /api/v1/admin/users/{id}/status ────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_status_disables_user() throws Exception {
        when(adminService.updateEnabled(eq(2L), eq(false), eq("admin")))
                .thenReturn(sampleDisabled(2L, "bob"));

        mvc.perform(patch("/api/v1/admin/users/2/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateStatusRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_status_forbidden_for_user_role() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/2/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateStatusRequest(false))))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/v1/admin/users/{id} ──────────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_user_returns_204() throws Exception {
        doNothing().when(adminService).deleteUser(eq(2L), eq("admin"));

        mvc.perform(delete("/api/v1/admin/users/2"))
                .andExpect(status().isNoContent());

        verify(adminService).deleteUser(2L, "admin");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_user_returns_404_when_not_found() throws Exception {
        doThrow(new UsernameNotFoundException("not found"))
                .when(adminService).deleteUser(eq(99L), eq("admin"));

        mvc.perform(delete("/api/v1/admin/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_user_forbidden_for_user_role() throws Exception {
        mvc.perform(delete("/api/v1/admin/users/2"))
                .andExpect(status().isForbidden());
    }
}
