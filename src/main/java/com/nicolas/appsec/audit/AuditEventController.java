package com.nicolas.appsec.audit;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AuditEventController {

    private final AuditEventRepository repo;

    public AuditEventController(AuditEventRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/v1/audit-events/recent")
    public List<AuditEvent> recent() {
        return repo.findAllByOrderByEventTimeDesc(PageRequest.of(0, 20)).getContent();
    }
}
