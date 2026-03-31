package com.nicolas.appsec.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RateLimitFilter extends OncePerRequestFilter {

    private final InMemoryRateLimiter limiter;
    private final TrustedProxyConfig trustedProxyConfig;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            InMemoryRateLimiter limiter,
            TrustedProxyConfig trustedProxyConfig,
            ObjectMapper objectMapper
    ) {
        this.limiter = limiter;
        this.trustedProxyConfig = trustedProxyConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) return true;

        return !(path.equals("/api/v1/ping") || path.equals("/api/v1/audit-events/recent"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();

        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) return remote;

        if (!trustedProxyConfig.isTrusted(remote)) return remote;

        String ip = xff.split(",")[0].trim();
        if (ip.matches("^[0-9a-fA-F:.]+$")) return ip;

        return remote;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = resolveClientIp(request);

        int windowSeconds = 60;
        int limit = path.equals("/api/v1/ping") ? 30 : 10;

        String key = path + "|" + ip;
        InMemoryRateLimiter.Decision d = limiter.allow(key, limit, windowSeconds);

        response.setHeader("RateLimit-Limit", String.valueOf(d.limit()));
        response.setHeader("RateLimit-Remaining", String.valueOf(d.remaining()));
        response.setHeader("RateLimit-Reset", String.valueOf(d.resetEpochSeconds()));

        if (!d.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(d.retryAfterSeconds()));
            response.setContentType("application/problem+json");

            ProblemDetail problem = ProblemDetail.forStatus(429);
            problem.setTitle("Too Many Requests");
            problem.setDetail("Rate limit exceeded for this client and endpoint.");
            problem.setProperty("retryAfterSeconds", d.retryAfterSeconds());
            problem.setProperty("path", request.getRequestURI());

            objectMapper.writeValue(response.getWriter(), problem);
            return;
        }

        chain.doFilter(request, response);
    }
}