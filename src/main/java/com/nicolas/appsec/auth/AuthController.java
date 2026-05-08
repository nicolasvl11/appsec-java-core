package com.nicolas.appsec.auth;

import com.nicolas.appsec.audit.AuditEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, refresh, and logout")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final JwtBlacklistService blacklistService;
    private final AuditEventService auditService;

    public AuthController(AuthService authService, JwtService jwtService,
                          JwtBlacklistService blacklistService, AuditEventService auditService) {
        this.authService      = authService;
        this.jwtService       = jwtService;
        this.blacklistService = blacklistService;
        this.auditService     = auditService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created — access + refresh tokens returned",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Username already taken")
    })
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login with username + password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — access + refresh tokens returned",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Blank username or password"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "423", description = "Account locked — too many failed attempts")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh access token",
            description = "Exchanges a valid refresh token for a new access token and rotated refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair issued",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout — invalidate access token",
            description = "Blacklists the Bearer token so it cannot be reused before its natural expiry.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token invalidated")
    })
    public void logout(HttpServletRequest request,
                       @AuthenticationPrincipal UserDetails principal) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token)) {
                blacklistService.blacklist(jwtService.extractJti(token), jwtService.extractExpiration(token));
            }
        }
        String actor = principal != null ? principal.getUsername() : "anonymous";
        auditService.recordSecurityEvent(actor, "logout", "/api/v1/auth/logout", Map.of());
    }
}
