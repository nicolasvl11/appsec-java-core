package com.nicolas.appsec.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Admin", description = "Endpoints restricted to the ADMIN role")
public class AdminController {

    @GetMapping("/api/v1/admin")
    @Operation(summary = "Admin status check", description = "Returns 200 for authenticated ADMIN users; 403 otherwise.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caller has ADMIN role"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not ADMIN")
    })
    public Map<String, Object> admin() {
        return Map.of(
                "status", "ok",
                "area", "admin"
        );
    }
}
