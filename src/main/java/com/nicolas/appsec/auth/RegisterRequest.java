package com.nicolas.appsec.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "New user registration payload")
public record RegisterRequest(
        @Schema(description = "Unique username (3-50 alphanumeric/_)", example = "alice")
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username may only contain letters, digits, and underscores")
        String username,

        @Schema(description = "Password (8-100 characters)", example = "s3cr3tP@ss")
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
        String password
) {}
