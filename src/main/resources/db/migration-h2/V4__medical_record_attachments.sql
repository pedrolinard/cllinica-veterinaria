CREATE TABLE medical_record_attachments (
    id UUID NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    medical_record_id UUID NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    content VARBINARY NOT NULL,
    CONSTRAINT fk_attachments_medical_record FOREIGN KEY (medical_record_id) REFERENCES medical_records (id)
);
CREATE INDEX idx_attachments_medical_record_id ON medical_record_attachments (medical_record_id);
