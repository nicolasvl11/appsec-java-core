package com.nicolas.appsec.auth;

import com.nicolas.appsec.audit.AuditEventService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final RefreshTokenService refreshTokenService;
    private final AuditEventService auditService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginAttemptService loginAttemptService,
            RefreshTokenService refreshTokenService,
            AuditEventService auditService
    ) {
        this.userRepository      = userRepository;
        this.passwordEncoder     = passwordEncoder;
        this.jwtService          = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.refreshTokenService = refreshTokenService;
        this.auditService        = auditService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }
        User user = new User(request.username(), passwordEncoder.encode(request.password()), Role.USER);
        userRepository.save(user);

        auditService.recordSecurityEvent(request.username(), "register", "/api/v1/auth/register", Map.of());

        String access  = jwtService.generateToken(user.getUsername(), user.getRole().name());
        String refresh = refreshTokenService.generate(user.getUsername());
        return new AuthResponse(access, refresh, user.getUsername(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        if (loginAttemptService.isLocked(request.username())) {
            throw new AccountLockedException(loginAttemptService.getRetryAfterSeconds(request.username()));
        }

        User user = userRepository.findByUsername(request.username()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.recordFailure(request.username());
            auditService.recordSecurityEvent(request.username(), "login_failure", "/api/v1/auth/login",
                    Map.of("reason", "invalid_credentials"));
            if (loginAttemptService.isLocked(request.username())) {
                throw new AccountLockedException(loginAttemptService.getRetryAfterSeconds(request.username()));
            }
            throw new BadCredentialsException("Invalid credentials");
        }

        loginAttemptService.reset(request.username());
        auditService.recordSecurityEvent(request.username(), "login_success", "/api/v1/auth/login", Map.of());

        String access  = jwtService.generateToken(user.getUsername(), user.getRole().name());
        String refresh = refreshTokenService.generate(user.getUsername());
        return new AuthResponse(access, refresh, user.getUsername(), user.getRole().name());
    }

    public AuthResponse refresh(String rawRefreshToken) {
        String username = refreshTokenService.verify(rawRefreshToken);
        refreshTokenService.revoke(rawRefreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        auditService.recordSecurityEvent(username, "token_refresh", "/api/v1/auth/refresh", Map.of());

        String newAccess   = jwtService.generateToken(username, user.getRole().name());
        String newRefresh  = refreshTokenService.generate(username);
        return new AuthResponse(newAccess, newRefresh, username, user.getRole().name());
    }
}
