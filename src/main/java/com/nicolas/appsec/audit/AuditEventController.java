package com.nicolas.appsec.audit;

import com.nicolas.appsec.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Audit", description = "Security audit event log")
public class AuditEventController {

    private final AuditEventRepository repo;

    public AuditEventController(AuditEventRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/v1/audit-events/recent")
    @Operation(summary = "Paginated audit events with optional filters",
            description = "Returns security audit events, newest first. Supports actor/action/target substring filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of audit events"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public PageResponse<AuditEvent> recent(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 100) String actor,
            @RequestParam(required = false) @Size(max = 100) String action,
            @RequestParam(required = false) @Size(max = 200) String target
    ) {
        String actorParam  = (actor  != null && !actor.isBlank())  ? actor.trim()  : null;
        String actionParam = (action != null && !action.isBlank()) ? action.trim() : null;
        String targetParam = (target != null && !target.isBlank()) ? target.trim() : null;

        if (actorParam == null && actionParam == null && targetParam == null) {
            return PageResponse.from(repo.findAllByOrderByEventTimeDesc(PageRequest.of(page, size)));
        }
        return PageResponse.from(repo.findFiltered(actorParam, actionParam, targetParam, PageRequest.of(page, size)));
    }
}
