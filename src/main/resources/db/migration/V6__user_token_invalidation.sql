-- Marca a partir de quando os tokens de um usuário deixam de valer. Setado ao
-- trocar o papel do usuário ou a senha, para que um JWT emitido antes dessa
-- mudança (que pode viver até 8h) seja rejeitado imediatamente.
-- NULL = nunca invalidado (comportamento anterior).
ALTER TABLE users ADD COLUMN tokens_invalid_before TIMESTAMP;
