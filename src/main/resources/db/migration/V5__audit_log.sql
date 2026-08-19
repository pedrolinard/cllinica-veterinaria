CREATE TABLE audit_log (
    id UUID NOT NULL PRIMARY KEY,
    performed_at TIMESTAMP NOT NULL,
    performed_by VARCHAR(255) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    detail VARCHAR(500)
);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_performed_at ON audit_log (performed_at);
