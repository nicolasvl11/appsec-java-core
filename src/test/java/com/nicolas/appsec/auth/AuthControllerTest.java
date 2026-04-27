package com.nicolas.appsec.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolas.appsec.audit.AuditEventService;
import com.nicolas.appsec.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuditEventService    auditEventService;
    @MockBean AuthService          authService;
    @MockBean JwtBlacklistService  blacklistService;

    // ── register ────────────────────────────────────────────────────────────

    @Test
    void register_returns_201_with_tokens() throws Exception {
        when(authService.register(any()))
                .thenReturn(new AuthResponse("tok123", "rt-mock", "alice", "USER"));

        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("alice", "password1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("tok123"))
                .andExpect(jsonPath("$.refreshToken").value("rt-mock"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_returns_409_when_username_taken() throws Exception {
        when(authService.register(any())).thenThrow(new UsernameAlreadyExistsException("alice"));

        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("alice", "password1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void register_rejects_blank_username() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("", "password1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void register_rejects_short_password() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("alice", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void register_rejects_username_with_special_chars() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("ali ce!", "password1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_returns_200_with_tokens() throws Exception {
        when(authService.login(any()))
                .thenReturn(new AuthResponse("tok456", "rt-mock", "alice", "USER"));

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("alice", "password1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok456"))
                .andExpect(jsonPath("$.refreshToken").value("rt-mock"));
    }

    @Test
    void login_returns_401_on_bad_credentials() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("bad"));

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("alice", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password."));
    }

    @Test
    void login_returns_423_when_account_locked() throws Exception {
        when(authService.login(any())).thenThrow(new AccountLockedException(300L));

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("alice", "pw"))))
                .andExpect(status().isLocked())
                .andExpect(header().string("Retry-After", "300"))
                .andExpect(jsonPath("$.title").value("Account Locked"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(300));
    }

    @Test
    void login_rejects_blank_fields() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    // ── refresh ──────────────────────────────────────────────────────────────

    @Test
    void refresh_returns_new_token_pair() throws Exception {
        when(authService.refresh("raw-rt"))
                .thenReturn(new AuthResponse("newAccess", "newRefresh", "alice", "USER"));

        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"raw-rt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("newAccess"))
                .andExpect(jsonPath("$.refreshToken").value("newRefresh"));
    }

    @Test
    void refresh_returns_401_on_invalid_token() throws Exception {
        when(authService.refresh(any())).thenThrow(new BadCredentialsException("invalid"));

        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_rejects_blank_token() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    // ── logout ───────────────────────────────────────────────────────────────

    @Test
    void logout_with_no_token_returns_204() throws Exception {
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
        verifyNoInteractions(blacklistService);
    }

    @Test
    void logout_with_invalid_token_skips_blacklist() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isNoContent());
        verifyNoInteractions(blacklistService);
    }
}
