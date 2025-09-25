-- Table pour stocker les demandes de changement de rôle
CREATE TABLE IF NOT EXISTS role_change_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requested_role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    reason TEXT,
    admin_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    processed_by UUID REFERENCES users(id)
);

-- Renommer la séquence pour correspondre à la convention Hibernate
-- La séquence est créée automatiquement avec BIGSERIAL, on la renomme ensuite
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'role_change_requests_id_seq') THEN
        ALTER SEQUENCE role_change_requests_id_seq RENAME TO role_change_requests_seq;
    END IF;
END $$;

-- Index pour améliorer les performances
CREATE INDEX idx_role_change_requests_user_id ON role_change_requests(user_id);
CREATE INDEX idx_role_change_requests_status ON role_change_requests(status);
CREATE INDEX idx_role_change_requests_created_at ON role_change_requests(created_at);
