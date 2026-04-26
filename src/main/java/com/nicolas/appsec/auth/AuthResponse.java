package com.nicolas.appsec.auth;

public record AuthResponse(String token, String username, String role) {}
