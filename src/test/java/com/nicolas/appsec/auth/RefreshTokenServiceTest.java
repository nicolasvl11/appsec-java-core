package com.nicolas.appsec.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository repo;
    RefreshTokenService service;

    @BeforeEach
    void setUp() { service = new RefreshTokenService(repo); }

    @Test
    void generate_persists_token_and_returns_raw() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(repo).deleteExpiredByUsername(anyString(), any());

        String raw = service.generate("alice");

        assertThat(raw).isNotBlank();
        ArgumentCaptor<RefreshToken> cap = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getUsername()).isEqualTo("alice");
        assertThat(cap.getValue().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void generate_returns_different_tokens_each_call() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(repo).deleteExpiredByUsername(anyString(), any());

        String t1 = service.generate("alice");
        String t2 = service.generate("alice");

        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void verify_returns_username_for_valid_token() {
        String raw  = "test-raw-token";
        String hash = RefreshTokenService.sha256(raw);
        RefreshToken rt = new RefreshToken(hash, "alice", Instant.now().plusSeconds(3600));
        when(repo.findByTokenHashAndRevokedFalse(hash)).thenReturn(Optional.of(rt));

        assertThat(service.verify(raw)).isEqualTo("alice");
    }

    @Test
    void verify_throws_on_unknown_token() {
        when(repo.findByTokenHashAndRevokedFalse(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("bad-token"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void verify_throws_on_expired_token() {
        String raw  = "expired-token";
        String hash = RefreshTokenService.sha256(raw);
        RefreshToken rt = new RefreshToken(hash, "alice", Instant.now().minusSeconds(1));
        when(repo.findByTokenHashAndRevokedFalse(hash)).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> service.verify(raw))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void revoke_marks_token_as_revoked() {
        String raw  = "to-revoke";
        String hash = RefreshTokenService.sha256(raw);
        RefreshToken rt = new RefreshToken(hash, "alice", Instant.now().plusSeconds(3600));
        when(repo.findByTokenHash(hash)).thenReturn(Optional.of(rt));

        service.revoke(raw);

        assertThat(rt.isRevoked()).isTrue();
    }

    @Test
    void sha256_is_deterministic() {
        assertThat(RefreshTokenService.sha256("hello"))
                .isEqualTo(RefreshTokenService.sha256("hello"))
                .hasSize(64);
    }
}
