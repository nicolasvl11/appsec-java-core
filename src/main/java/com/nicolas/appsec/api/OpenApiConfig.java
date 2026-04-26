package com.nicolas.appsec.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AppSec Java Core API",
                version = "1.0",
                description = "Enterprise security reference: JWT/OAuth2 auth, rate limiting, audit logging, Kubernetes-ready.",
                contact = @Contact(name = "Nicolas Velasquez", email = "nicolas.velasquez1233@gmail.com")
        ),
        security = @SecurityRequirement(name = "bearerAuth"),
        servers = @Server(url = "/", description = "Current host")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "RS256 JWT token. Obtain via POST /api/v1/auth/login or OAuth2 flow."
)
public class OpenApiConfig {
}
