package com.nicolas.appsec.auth;

import com.nicolas.appsec.audit.AuditEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock LoginAttemptService loginAttemptService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock AuditEventService auditService;
    @InjectMocks AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(jwtService.generateToken(anyString(), anyString())).thenReturn("access-token");
        lenient().when(refreshTokenService.generate(anyString())).thenReturn("refresh-token");
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_creates_user_and_returns_tokens() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");

        AuthResponse resp = authService.register(new RegisterRequest("alice", "pass123"));

        assertThat(resp.token()).isEqualTo("access-token");
        assertThat(resp.refreshToken()).isEqualTo("refresh-token");
        assertThat(resp.username()).isEqualTo("alice");
        assertThat(resp.role()).isEqualTo("USER");

        verify(userRepository).save(argThat(u -> u.getUsername().equals("alice")
                && u.getPassword().equals("hashed")
                && u.getRole() == Role.USER));
        verify(auditService).recordSecurityEvent(eq("alice"), eq("register"),
                eq("/api/v1/auth/register"), any());
    }

    @Test
    void register_throws_conflict_when_username_taken() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "pass123")))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_returns_tokens_on_valid_credentials() {
        User alice = new User("alice", "hashed", Role.USER);
        when(loginAttemptService.isLocked("alice")).thenReturn(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(passwordEncoder.matches("pass123", "hashed")).thenReturn(true);

        AuthResponse resp = authService.login(new LoginRequest("alice", "pass123"));

        assertThat(resp.token()).isEqualTo("access-token");
        assertThat(resp.username()).isEqualTo("alice");
        verify(loginAttemptService).reset("alice");
        verify(userRepository).save(alice);
        assertThat(alice.getLastLogin()).isNotNull();
        verify(auditService).recordSecurityEvent(eq("alice"), eq("login_success"),
                eq("/api/v1/auth/login"), any());
    }

    @Test
    void login_throws_disabled_when_account_is_disabled() {
        User alice = new User("alice", "hashed", Role.USER);
        alice.updateEnabled(false);
        when(loginAttemptService.isLocked("alice")).thenReturn(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(passwordEncoder.matches("pass123", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "pass123")))
                .isInstanceOf(DisabledException.class);

        verify(userRepository, never()).save(any());
        verify(auditService).recordSecurityEvent(eq("alice"), eq("login_failure"),
                eq("/api/v1/auth/login"), argThat(m -> "account_disabled".equals(m.get("reason"))));
    }

    @Test
    void login_throws_bad_credentials_on_wrong_password() {
        User alice = new User("alice", "hashed", Role.USER);
        when(loginAttemptService.isLocked("alice")).thenReturn(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService).recordFailure("alice");
        verify(auditService).recordSecurityEvent(eq("alice"), eq("login_failure"),
                eq("/api/v1/auth/login"), argThat(m -> "invalid_credentials".equals(m.get("reason"))));
    }

    @Test
    void login_throws_bad_credentials_when_user_not_found() {
        when(loginAttemptService.isLocked("ghost")).thenReturn(false);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "pass")))
                .isInstanceOf(BadCredentialsException.class);

        verify(auditService).recordSecurityEvent(eq("ghost"), eq("login_failure"),
                anyString(), any());
    }

    @Test
    void login_throws_locked_when_account_is_locked() {
        when(loginAttemptService.isLocked("alice")).thenReturn(true);
        when(loginAttemptService.getRetryAfterSeconds("alice")).thenReturn(900L);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "pass")))
                .isInstanceOf(AccountLockedException.class);

        verifyNoInteractions(userRepository, auditService);
    }

    @Test
    void login_throws_locked_after_final_failed_attempt() {
        User alice = new User("alice", "hashed", Role.USER);
        when(loginAttemptService.isLocked("alice")).thenReturn(false).thenReturn(true);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        when(loginAttemptService.getRetryAfterSeconds("alice")).thenReturn(900L);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(AccountLockedException.class);
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_issues_new_token_pair() {
        User alice = new User("alice", "hashed", Role.ADMIN);
        when(refreshTokenService.verify("rt")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        AuthResponse resp = authService.refresh("rt");

        assertThat(resp.token()).isEqualTo("access-token");
        assertThat(resp.role()).isEqualTo("ADMIN");
        verify(refreshTokenService).revoke("rt");
        verify(auditService).recordSecurityEvent(eq("alice"), eq("token_refresh"),
                eq("/api/v1/auth/refresh"), any());
    }

    @Test
    void refresh_throws_when_user_deleted_after_token_issued() {
        when(refreshTokenService.verify("rt")).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("rt"))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── loadUserByUsername ────────────────────────────────────────────────────

    @Test
    void loadUserByUsername_returns_user_when_found() {
        User alice = new User("alice", "hashed", Role.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertThat(authService.loadUserByUsername("alice").getUsername()).isEqualTo("alice");
    }

    @Test
    void loadUserByUsername_throws_when_not_found() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
