package com.nicolas.appsec.auth;

import java.time.Instant;

public record UserProfileResponse(Long id, String username, String role, Instant createdAt,
                                   String email, String provider) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getEmail(),
                user.getProvider()
        );
    }
}
