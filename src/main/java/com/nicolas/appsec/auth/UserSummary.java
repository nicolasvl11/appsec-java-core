package com.nicolas.appsec.auth;

import java.time.Instant;

public record UserSummary(Long id, String username, String role, Instant createdAt, String email, String provider) {

    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getEmail(),
                user.getProvider()
        );
    }
}
