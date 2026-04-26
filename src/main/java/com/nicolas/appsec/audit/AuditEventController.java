package com.nicolas.appsec.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Audit", description = "Security audit event log")
public class AuditEventController {

    private final AuditEventRepository repo;

    public AuditEventController(AuditEventRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/v1/audit-events/recent")
    @Operation(summary = "Recent audit events",
            description = "Returns the 20 most recent security audit events (login attempts, rate-limit blocks, etc.).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of audit events (may be empty)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public List<AuditEvent> recent() {
        return repo.findAllByOrderByEventTimeDesc(PageRequest.of(0, 20)).getContent();
    }
}
