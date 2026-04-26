package com.nicolas.appsec.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // Base64 of "this-is-a-very-secret-jwt-key-for-dev" (37 bytes = 296 bits, valid for HS256)
    private static final String SECRET = "dGhpcy1pcy1hLXZlcnktc2VjcmV0LWp3dC1rZXktZm9yLWRldg==";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 86_400_000L);
    }

    @Test
    void generated_token_is_valid() {
        String token = jwtService.generateToken("alice", "USER");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void extract_username_matches_subject() {
        String token = jwtService.generateToken("alice", "USER");
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void expired_token_is_invalid() {
        JwtService shortLived = new JwtService(SECRET, -1L); // expiry in the past
        String token = shortLived.generateToken("alice", "USER");
        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void tampered_token_is_invalid() {
        String token = jwtService.generateToken("alice", "USER");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void garbage_string_is_invalid() {
        assertThat(jwtService.isValid("not.a.jwt")).isFalse();
    }
}
