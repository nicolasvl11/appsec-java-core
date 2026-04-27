package com.nicolas.appsec.auth;

public record AuthResponse(String token, String refreshToken, String username, String role) {}
