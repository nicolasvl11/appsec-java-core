package com.nicolas.appsec.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    protected RefreshToken() {}

    public RefreshToken(String tokenHash, String username, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.username  = username;
        this.expiresAt = expiresAt;
    }

    public String getTokenHash()  { return tokenHash; }
    public String getUsername()   { return username; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked()    { return revoked; }

    public void revoke() { this.revoked = true; }
}
