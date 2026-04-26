package com.nicolas.appsec.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtService jwtService;
    private final String redirectUri;

    public OAuth2LoginSuccessHandler(UserService userService, JwtService jwtService, String redirectUri) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = token.getPrincipal();
        String registrationId = token.getAuthorizedClientRegistrationId();

        String providerUserId = oauth2User.getName();
        String email = getAttribute(oauth2User, "email");
        String name = getAttribute(oauth2User, "name");
        if (name == null) {
            name = getAttribute(oauth2User, "login"); // GitHub uses "login"
        }

        User user = userService.findOrCreateOAuth2User(registrationId, providerUserId, email, name);
        String jwt = jwtService.generateToken(user.getUsername(), user.getRole().name());

        String target = redirectUri + "?token=" + jwt;
        getRedirectStrategy().sendRedirect(request, response, target);
    }

    private static String getAttribute(OAuth2User user, String name) {
        Object value = user.getAttribute(name);
        return value != null ? value.toString() : null;
    }
}
