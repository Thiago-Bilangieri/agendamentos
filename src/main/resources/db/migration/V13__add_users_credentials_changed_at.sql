-- Instante (em UTC) da última mudança de email ou password, ou da eliminação da conta.
-- Os tokens JWT emitidos antes deste instante deixam de ser aceites (ver JwtAuthenticationFilter).
-- NULL: as credenciais nunca mudaram e todos os tokens ainda válidos são aceites.
ALTER TABLE users ADD COLUMN credentials_changed_at TIMESTAMP;
