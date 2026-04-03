package com.nicolas.appsec.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolas.appsec.audit.AuditEventService;
import com.nicolas.appsec.audit.AuditLoggingFilter;
import com.nicolas.appsec.ratelimit.InMemoryRateLimiter;
import com.nicolas.appsec.ratelimit.RateLimitFilter;
import com.nicolas.appsec.ratelimit.TrustedProxyConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.time.Clock;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    @Bean
    AuditLoggingFilter auditLoggingFilter(AuditEventService service, TrustedProxyConfig trustedProxyConfig) {
        return new AuditLoggingFilter(service, trustedProxyConfig);
    }

    @Bean
    InMemoryRateLimiter rateLimiter() {
        return new InMemoryRateLimiter(Clock.systemUTC());
    }

    @Bean
    TrustedProxyConfig trustedProxyConfig(
            @Value("${app.trusted-proxies:127.0.0.1,::1}") String raw
    ) {
        Set<String> trusted = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        return new TrustedProxyConfig(trusted);
    }

    @Bean
    RateLimitFilter rateLimitFilter(
            InMemoryRateLimiter limiter,
            TrustedProxyConfig trustedProxyConfig,
            ObjectMapper objectMapper
    ) {
        return new RateLimitFilter(limiter, trustedProxyConfig, objectMapper);
    }

    @Bean
    RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    RestAccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuditLoggingFilter auditLoggingFilter,
            RateLimitFilter rateLimitFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/ping").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        http.addFilterAfter(auditLoggingFilter, BasicAuthenticationFilter.class);
        http.addFilterAfter(rateLimitFilter, AuditLoggingFilter.class);

        return http.build();
    }
}