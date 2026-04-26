package com.nicolas.appsec.auth;

import com.nicolas.appsec.audit.AuditEventService;
import com.nicolas.appsec.security.SecurityConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that JwtAuthenticationFilter correctly authenticates/rejects requests
 * and that the security chain enforces authorization for protected endpoints.
 */
@WebMvcTest(controllers = {com.nicolas.appsec.api.PingController.class,
                            com.nicolas.appsec.api.AdminController.class})
@Import(SecurityConfig.class)
class JwtAuthenticationFilterIntegrationTest {

    @Autowired MockMvc mvc;
    @MockBean AuditEventService auditEventService;

    static String validToken;
    static String adminToken;
    static String expiredToken;

    @BeforeAll
    static void setupTokens() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        String priv = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String pub  = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        JwtService svc = new JwtService(priv, pub, 86_400_000L);
        validToken   = svc.generateToken("alice", "USER");
        adminToken   = svc.generateToken("admin", "ADMIN");
        expiredToken = new JwtService(priv, pub, -1L).generateToken("alice", "USER");
    }

    // ── /api/v1/ping (public) ────────────────────────────────────────────────

    @Test
    void ping_with_no_token_returns_200() throws Exception {
        doNothing().when(auditEventService).recordHttpEvent(any(), any(), any(), anyInt(), any(), any(), anyLong(), any());
        mvc.perform(get("/api/v1/ping")).andExpect(status().isOk());
    }

    @Test
    void ping_with_valid_token_returns_200() throws Exception {
        doNothing().when(auditEventService).recordHttpEvent(any(), any(), any(), anyInt(), any(), any(), anyLong(), any());
        mvc.perform(get("/api/v1/ping").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void ping_with_expired_token_still_200_because_public() throws Exception {
        doNothing().when(auditEventService).recordHttpEvent(any(), any(), any(), anyInt(), any(), any(), anyLong(), any());
        mvc.perform(get("/api/v1/ping").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isOk());
    }

    // ── /api/v1/admin (ADMIN only) ──────────────────────────────────────────

    @Test
    void admin_with_no_token_returns_401() throws Exception {
        mvc.perform(get("/api/v1/admin")).andExpect(status().isUnauthorized());
    }

    @Test
    void admin_with_user_role_token_returns_401_because_unknown_to_in_memory_uds() throws Exception {
        // The token carries USER role but the InMemoryUserDetailsManager (test UDS)
        // doesn't know user "alice" → filter skips auth → 401
        mvc.perform(get("/api/v1/admin").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_with_expired_token_returns_401() throws Exception {
        mvc.perform(get("/api/v1/admin").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_with_garbage_bearer_returns_401() throws Exception {
        mvc.perform(get("/api/v1/admin").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }
}
