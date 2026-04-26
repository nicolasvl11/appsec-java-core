package com.nicolas.appsec.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login credentials")
public record LoginRequest(
        @Schema(description = "Registered username", example = "alice")
        @NotBlank(message = "username is required")
        String username,

        @Schema(description = "Account password", example = "s3cr3tP@ss")
        @NotBlank(message = "password is required")
        String password
) {}
