package com.nicolas.appsec.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolas.appsec.audit.AuditEventService;
import com.nicolas.appsec.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuditEventService auditEventService;
    @MockBean UserService userService;

    // ── GET /me ─────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    void get_me_returns_profile() throws Exception {
        when(userService.getProfile("alice"))
                .thenReturn(new UserProfileResponse(1L, "alice", "USER", Instant.parse("2025-01-01T00:00:00Z")));

        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void get_me_unauthenticated_returns_401() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── PATCH /me/password ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    void change_password_returns_204() throws Exception {
        doNothing().when(userService).changePassword(eq("alice"), any());

        mvc.perform(patch("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ChangePasswordRequest("oldPass1", "newPass123", "newPass123"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "alice")
    void change_password_rejects_mismatch_confirm() throws Exception {
        mvc.perform(patch("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ChangePasswordRequest("oldPass1", "newPass123", "different"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser(username = "alice")
    void change_password_rejects_short_new_password() throws Exception {
        mvc.perform(patch("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ChangePasswordRequest("oldPass1", "short", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser(username = "alice")
    void change_password_rejects_blank_current_password() throws Exception {
        mvc.perform(patch("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ChangePasswordRequest("", "newPass123", "newPass123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser(username = "alice")
    void change_password_returns_401_when_current_is_wrong() throws Exception {
        doThrow(new BadCredentialsException("wrong")).when(userService).changePassword(any(), any());

        mvc.perform(patch("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ChangePasswordRequest("wrongPass", "newPass123", "newPass123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void change_password_unauthenticated_returns_401() throws Exception {
        mvc.perform(patch("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ChangePasswordRequest("old", "newPass123", "newPass123"))))
                .andExpect(status().isUnauthorized());
    }
}
