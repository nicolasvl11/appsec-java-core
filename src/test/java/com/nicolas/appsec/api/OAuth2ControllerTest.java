package com.nicolas.appsec.api;

import com.nicolas.appsec.auth.OAuthUserInfoResponse;
import com.nicolas.appsec.auth.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OAuth2Controller.class)
@AutoConfigureMockMvc(addFilters = false)
class OAuth2ControllerTest {

    @Autowired MockMvc mvc;
    @MockBean UserService userService;

    @Test
    @WithMockUser(username = "google_123")
    void userinfo_returns_oauth2_user_info() throws Exception {
        when(userService.getOAuthUserInfo("google_123")).thenReturn(
                new OAuthUserInfoResponse("123", "user@example.com", "google_123", "google", true));

        mvc.perform(get("/api/v1/oauth2/userinfo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("123"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.provider").value("google"))
                .andExpect(jsonPath("$.oauth2User").value(true));
    }

    @Test
    @WithMockUser(username = "regularuser")
    void userinfo_returns_non_oauth2_user_info() throws Exception {
        when(userService.getOAuthUserInfo("regularuser")).thenReturn(
                new OAuthUserInfoResponse(null, null, "regularuser", null, false));

        mvc.perform(get("/api/v1/oauth2/userinfo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oauth2User").value(false))
                .andExpect(jsonPath("$.provider").isEmpty());
    }
}
