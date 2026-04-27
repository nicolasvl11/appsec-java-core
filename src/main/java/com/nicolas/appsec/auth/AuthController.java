package com.nicolas.appsec.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, and token management")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final JwtBlacklistService blacklistService;

    public AuthController(AuthService authService, JwtService jwtService, JwtBlacklistService blacklistService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Register a new user", description = "Creates a local account and returns an RS256 JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created, JWT returned",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error (username/password constraints)"),
            @ApiResponse(responseCode = "409", description = "Username already taken")
    })
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login with username + password", description = "Returns an RS256 JWT valid for 24 hours.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, JWT returned",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Blank username or password"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout — invalidate JWT",
            description = "Blacklists the Bearer token so it cannot be reused before its natural expiry.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token invalidated"),
            @ApiResponse(responseCode = "401", description = "No valid token provided")
    })
    public void logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return;
        }
        String token = header.substring(7);
        if (jwtService.isValid(token)) {
            blacklistService.blacklist(jwtService.extractJti(token), jwtService.extractExpiration(token));
        }
    }
}
