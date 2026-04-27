package com.nicolas.appsec.auth;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    static String privateKeyB64;
    static String publicKeyB64;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        privateKeyB64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        publicKeyB64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(privateKeyB64, publicKeyB64, 86_400_000L);
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
        JwtService shortLived = new JwtService(privateKeyB64, publicKeyB64, -1L);
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

    @Test
    void extract_jti_is_non_blank_uuid() {
        String token = jwtService.generateToken("alice", "USER");
        String jti = jwtService.extractJti(token);
        assertThat(jti).isNotBlank();
        assertThat(jti).matches("[0-9a-f\\-]{36}");
    }

    @Test
    void two_tokens_have_different_jti() {
        String t1 = jwtService.generateToken("alice", "USER");
        String t2 = jwtService.generateToken("alice", "USER");
        assertThat(jwtService.extractJti(t1)).isNotEqualTo(jwtService.extractJti(t2));
    }

    @Test
    void extract_expiration_is_in_the_future() {
        String token = jwtService.generateToken("alice", "USER");
        Instant expiry = jwtService.extractExpiration(token);
        assertThat(expiry).isAfter(Instant.now());
    }

    @Test
    void token_signed_with_different_key_is_invalid() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair other = gen.generateKeyPair();
        JwtService otherService = new JwtService(
                Base64.getEncoder().encodeToString(other.getPrivate().getEncoded()),
                Base64.getEncoder().encodeToString(other.getPublic().getEncoded()),
                86_400_000L
        );
        String foreignToken = otherService.generateToken("alice", "USER");
        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }
}
