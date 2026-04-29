package com.nicolas.appsec.auth;

import com.nicolas.appsec.audit.AuditEventService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditEventService auditService) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService    = auditService;
    }

    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
        return UserProfileResponse.from(user);
    }

    public OAuthUserInfoResponse getOAuthUserInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return OAuthUserInfoResponse.from(user);
    }

    @Transactional
    public User findOrCreateOAuth2User(String provider, String providerUserId, String email, String displayName) {
        return userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseGet(() -> {
                    String username = provider + "_" + providerUserId;
                    User user = new User(username, email, provider, providerUserId, Role.USER);
                    return userRepository.save(user);
                });
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        if (user.isOAuth2User()) {
            throw new BadCredentialsException("Password change not supported for OAuth2 accounts.");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        auditService.recordSecurityEvent(username, "password_change", "/api/v1/users/me/password", Map.of());
    }
}
