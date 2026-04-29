-- Performance indexes for audit event filtering and sorting
CREATE INDEX IF NOT EXISTS idx_audit_event_event_time ON audit_event(event_time DESC);
CREATE INDEX IF NOT EXISTS idx_audit_event_actor      ON audit_event(actor);
CREATE INDEX IF NOT EXISTS idx_audit_event_action     ON audit_event(action);
CREATE INDEX IF NOT EXISTS idx_audit_event_target     ON audit_event(target);
