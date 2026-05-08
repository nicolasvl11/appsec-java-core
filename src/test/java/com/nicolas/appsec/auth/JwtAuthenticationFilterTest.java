package com.nicolas.appsec.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    JwtService jwtService;
    UserDetailsService userDetailsService;
    JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        var gen = java.security.KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        var pair = gen.generateKeyPair();
        String priv = java.util.Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String pub  = java.util.Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        jwtService = new JwtService(priv, pub, 86_400_000L);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, Optional.empty());
        SecurityContextHolder.clearContext();
    }

    @Test
    void valid_token_for_enabled_user_sets_authentication() throws Exception {
        String token = jwtService.generateToken("alice", "USER");
        var userDetails = new org.springframework.security.core.userdetails.User(
                "alice", "hashed", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
    }

    @Test
    void valid_token_for_disabled_user_does_not_set_authentication() throws Exception {
        String token = jwtService.generateToken("alice", "USER");
        var disabled = new org.springframework.security.core.userdetails.User(
                "alice", "hashed", false, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(disabled);

        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void no_token_does_not_set_authentication() throws Exception {
        var req = new MockHttpServletRequest();
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void expired_token_does_not_set_authentication() throws Exception {
        var gen = java.security.KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        var pair = gen.generateKeyPair();
        String priv = java.util.Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String pub  = java.util.Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String expired = new JwtService(priv, pub, -1L).generateToken("alice", "USER");

        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + expired);
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
    }
}
