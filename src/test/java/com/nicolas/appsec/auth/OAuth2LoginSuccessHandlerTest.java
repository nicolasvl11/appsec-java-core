package com.nicolas.appsec.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock UserService userService;
    @Mock JwtService jwtService;

    OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2LoginSuccessHandler(userService, jwtService,
                "http://localhost:3000/oauth2/redirect");
    }

    @Test
    void redirects_to_frontend_with_jwt_token() throws Exception {
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(),
                Map.of("sub", "google-123", "email", "user@example.com", "name", "Test User"),
                "sub"
        );
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oauth2User, List.of(), "google");

        User mockUser = new User("google_google-123", "user@example.com", "google", "google-123", Role.USER);
        when(userService.findOrCreateOAuth2User(eq("google"), eq("google-123"), any(), any()))
                .thenReturn(mockUser);
        when(jwtService.generateToken("google_google-123", "USER")).thenReturn("mock-jwt-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/oauth2/redirect?token=mock-jwt-token");
    }

    @Test
    void creates_user_with_correct_provider_and_id() throws Exception {
        OAuth2User githubUser = new DefaultOAuth2User(
                List.of(),
                Map.of("id", "456", "login", "octocat", "email", "octocat@github.com"),
                "id"
        );
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(githubUser, List.of(), "github");

        User mockUser = new User("github_456", "octocat@github.com", "github", "456", Role.USER);
        when(userService.findOrCreateOAuth2User("github", "456", "octocat@github.com", "octocat"))
                .thenReturn(mockUser);
        when(jwtService.generateToken("github_456", "USER")).thenReturn("github-jwt");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).contains("token=github-jwt");
    }
}
