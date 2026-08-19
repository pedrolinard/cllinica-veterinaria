-- Suporte a logout real: tokens revogados ficam aqui até expirarem por conta
-- própria, quando um job de limpeza os remove.
CREATE TABLE revoked_tokens (
    jti VARCHAR(64) NOT NULL PRIMARY KEY,
    expires_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens (expires_at);
