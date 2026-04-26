package com.nicolas.appsec.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolas.appsec.audit.AuditEventService;
import com.nicolas.appsec.audit.AuditLoggingFilter;
import com.nicolas.appsec.auth.JwtAuthenticationFilter;
import com.nicolas.appsec.auth.JwtService;
import com.nicolas.appsec.ratelimit.InMemoryRateLimiter;
import com.nicolas.appsec.ratelimit.RateLimiter;
import com.nicolas.appsec.ratelimit.RateLimitFilter;
import com.nicolas.appsec.ratelimit.TrustedProxyConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtService jwtService(
            @Value("${app.jwt.private-key}") String privateKey,
            @Value("${app.jwt.public-key}") String publicKey,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        return new JwtService(privateKey, publicKey, expirationMs);
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000}") String rawOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(rawOrigins.split(","))
                .map(String::trim).collect(Collectors.toList()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

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
            RateLimiter limiter,
            TrustedProxyConfig trustedProxyConfig,
            ObjectMapper objectMapper
    ) {
        return new RateLimitFilter(limiter, trustedProxyConfig, objectMapper);
    }

    @Bean
    RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
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
            RestAccessDeniedHandler restAccessDeniedHandler,
            RequestIdFilter requestIdFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000)
                                .requestMatcher(r -> true)
                        )
                        .cacheControl(Customizer.withDefaults())
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Content-Security-Policy", "default-src 'self'"
                        ))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/ping").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/admin").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/**").authenticated()
                        .requestMatchers("/api/v1/audit-events/**").authenticated()
                        .anyRequest().permitAll()
                );

        // Filter ordering: RequestId → JWT → [Spring Security] → AuditLogging → RateLimit
        http.addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(jwtAuthenticationFilter, RequestIdFilter.class);
        http.addFilterAfter(auditLoggingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(rateLimitFilter, AuditLoggingFilter.class);

        return http.build();
    }
}
