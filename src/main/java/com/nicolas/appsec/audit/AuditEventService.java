package com.nicolas.appsec.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditEventService {

    private final AuditEventRepository repo;
    private final ObjectMapper mapper;

    public AuditEventService(AuditEventRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Transactional
    public void recordHttpEvent(
            String actor,
            String method,
            String path,
            int status,
            String ip,
            String userAgent,
            long durationMs,
            String requestId
    ) {
        AuditEvent e = new AuditEvent();
        e.setEventTime(Instant.now());
        e.setActor(actor == null || actor.isBlank() ? "anonymous" : actor);
        e.setAction("http_request");
        e.setTarget(path);

        try {
            JsonNode metaJson = mapper.valueToTree(Map.of(
                    "method", method,
                    "status", status,
                    "ip", ip == null ? "" : ip,
                    "userAgent", userAgent == null ? "" : userAgent,
                    "durationMs", durationMs,
                    "requestId", requestId == null ? "" : requestId
            ));
            e.setMeta(metaJson);
        } catch (Exception ex) {
            e.setMeta(mapper.valueToTree(Map.of("error", "meta_serialization_failed")));
        }

        repo.save(e);
    }

    // Commits independently — audit trail survives outer transaction rollbacks
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSecurityEvent(String actor, String action, String target, Map<String, Object> meta) {
        AuditEvent e = new AuditEvent();
        e.setEventTime(Instant.now());
        e.setActor(actor == null || actor.isBlank() ? "anonymous" : actor);
        e.setAction(action);
        e.setTarget(target);

        try {
            e.setMeta(mapper.valueToTree(meta != null ? meta : Map.of()));
        } catch (Exception ex) {
            e.setMeta(mapper.valueToTree(Map.of("error", "meta_serialization_failed")));
        }

        repo.save(e);
    }
}
