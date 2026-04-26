package com.nicolas.appsec.auth;

public record OAuthUserInfoResponse(
        String sub,
        String email,
        String name,
        String provider,
        boolean oauth2User
) {
    public static OAuthUserInfoResponse from(User user) {
        return new OAuthUserInfoResponse(
                user.getProviderUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getProvider(),
                user.isOAuth2User()
        );
    }
}
