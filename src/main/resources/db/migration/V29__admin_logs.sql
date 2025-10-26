-- Table pour les logs d'actions administrateur (si elle n'existe pas déjà)
CREATE TABLE IF NOT EXISTS admin_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(20) NOT NULL, -- 'USER', 'LISTING', etc.
    target_id UUID NOT NULL,
    reason TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index pour améliorer les performances (si ils n'existent pas déjà)
CREATE INDEX IF NOT EXISTS idx_admin_logs_admin_id ON admin_logs(admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_logs_target ON admin_logs(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_admin_logs_created_at ON admin_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_admin_logs_action ON admin_logs(action);

-- Table pour les tokens d'impersonation (si elle n'existe pas déjà)
CREATE TABLE IF NOT EXISTS impersonation_tokens (
    id BIGSERIAL PRIMARY KEY,
    admin_id UUID NOT NULL REFERENCES users(id),
    target_user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(255) NOT NULL,
    reason TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index pour les tokens d'impersonation (si ils n'existent pas déjà)
CREATE INDEX IF NOT EXISTS idx_impersonation_tokens_admin_id ON impersonation_tokens(admin_id);
CREATE INDEX IF NOT EXISTS idx_impersonation_tokens_target_user_id ON impersonation_tokens(target_user_id);
CREATE INDEX IF NOT EXISTS idx_impersonation_tokens_token_hash ON impersonation_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_impersonation_tokens_expires_at ON impersonation_tokens(expires_at);
