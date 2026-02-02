package com.nicolas.appsec.security;

import com.nicolas.appsec.audit.AuditEventService;
import com.nicolas.appsec.audit.AuditLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    AuditLoggingFilter auditLoggingFilter(AuditEventService service) {
        return new AuditLoggingFilter(service);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuditLoggingFilter auditLoggingFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/ping").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .addFilterAfter(auditLoggingFilter, BasicAuthenticationFilter.class);

        return http.build();
    }
}
