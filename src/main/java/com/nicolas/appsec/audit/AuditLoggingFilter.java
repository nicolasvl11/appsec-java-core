package com.nicolas.appsec.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditEventService service;

    public AuditLoggingFilter(AuditEventService service) {
        this.service = service;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();
        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);

        try {
            chain.doFilter(request, wrapped);
        } finally {
            long durationMs = System.currentTimeMillis() - start;

            String actor = "anonymous";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                actor = auth.getName();
            }

            service.recordHttpEvent(
                actor,
                request.getMethod(),
                request.getRequestURI(),
                wrapped.getStatus(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                durationMs
            );

            wrapped.copyBodyToResponse();
        }
    }
}
