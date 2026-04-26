package com.nicolas.appsec.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "currentPassword is required")
        String currentPassword,

        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 100, message = "newPassword must be between 8 and 100 characters")
        String newPassword,

        @NotBlank(message = "confirmPassword is required")
        String confirmPassword
) {
    @AssertTrue(message = "newPassword and confirmPassword must match")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
