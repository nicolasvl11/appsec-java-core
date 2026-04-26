package com.nicolas.appsec.api;

import com.nicolas.appsec.auth.OAuthUserInfoResponse;
import com.nicolas.appsec.auth.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oauth2")
@Tag(name = "OAuth2", description = "OAuth2/OIDC identity information for the current user")
public class OAuth2Controller {

    private final UserService userService;

    public OAuth2Controller(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/userinfo")
    @Operation(summary = "OAuth2 user info",
            description = "Returns OIDC-like identity for the authenticated user. " +
                    "Fields `sub` and `provider` are non-null only for OAuth2 accounts.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User info returned",
                    content = @Content(schema = @Schema(implementation = OAuthUserInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public OAuthUserInfoResponse userinfo(@AuthenticationPrincipal UserDetails principal) {
        return userService.getOAuthUserInfo(principal.getUsername());
    }
}
