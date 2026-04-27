package com.nicolas.appsec.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RefreshTokenService {

    static final long TTL_DAYS = 7;

    private final RefreshTokenRepository repo;

    public RefreshTokenService(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public String generate(String username) {
        repo.deleteExpiredByUsername(username, Instant.now());

        String raw  = UUID.randomUUID() + "-" + UUID.randomUUID();
        String hash = sha256(raw);

        repo.save(new RefreshToken(hash, username, Instant.now().plus(TTL_DAYS, ChronoUnit.DAYS)));
        return raw;
    }

    @Transactional
    public String verify(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken rt = repo.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        if (rt.getExpiresAt().isBefore(Instant.now())) {
            rt.revoke();
            throw new BadCredentialsException("Refresh token expired");
        }
        return rt.getUsername();
    }

    @Transactional
    public void revoke(String rawToken) {
        repo.findByTokenHash(sha256(rawToken)).ifPresent(RefreshToken::revoke);
    }

    static String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
