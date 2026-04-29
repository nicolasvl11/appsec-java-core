package com.nicolas.appsec.auth;

import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull(message = "role is required")
        Role role
) {}
