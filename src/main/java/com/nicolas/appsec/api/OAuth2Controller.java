package com.nicolas.appsec.api;

import com.nicolas.appsec.auth.OAuthUserInfoResponse;
import com.nicolas.appsec.auth.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oauth2")
public class OAuth2Controller {

    private final UserService userService;

    public OAuth2Controller(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/userinfo")
    public OAuthUserInfoResponse userinfo(@AuthenticationPrincipal UserDetails principal) {
        return userService.getOAuthUserInfo(principal.getUsername());
    }
}
