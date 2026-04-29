package com.nicolas.appsec.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolas.appsec.audit.AuditEventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.Map;

public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final AuditEventService auditService;

    public RestAccessDeniedHandler(ObjectMapper objectMapper, AuditEventService auditService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
        auditService.recordSecurityEvent(actor, "permission_denied", request.getRequestURI(),
                Map.of("method", request.getMethod()));

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json");

        ProblemDetail problem = ProblemDetail.forStatus(HttpServletResponse.SC_FORBIDDEN);
        problem.setTitle("Forbidden");
        problem.setDetail("You do not have permission to access this resource.");
        problem.setProperty("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), problem);
    }
}
