package com.nicolas.appsec.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Public", description = "Unauthenticated endpoints")
public class PingController {

    @GetMapping("/api/v1/ping")
    @SecurityRequirements
    @Operation(
            summary = "Health / liveness ping",
            description = "Returns service status and current server timestamp. No authentication required."
    )
    @ApiResponse(responseCode = "200", description = "Service is up",
            content = @Content(examples = @ExampleObject(
                    value = """
                            {"status":"ok","service":"appsec-java-core","time":"2025-01-01T00:00:00Z"}
                            """)))
    public Map<String, Object> ping() {
        return Map.of(
            "status", "ok",
            "service", "appsec-java-core",
            "time", Instant.now().toString()
        );
    }
}
