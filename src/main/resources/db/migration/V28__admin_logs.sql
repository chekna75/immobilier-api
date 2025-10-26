-- Cette migration corrige la table admin_logs pour être cohérente avec V2
-- et ajoute les colonnes manquantes (ip, user_agent)

-- Supprimer la table si elle existe déjà (pour éviter les conflits)
DROP TABLE IF EXISTS admin_logs;

-- Recréer la table avec le bon schéma
CREATE TABLE admin_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    admin_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(60),
    target_id UUID,
    details JSONB,
    ip VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Recréer l'index
CREATE INDEX IF NOT EXISTS ix_admin_logs_target ON admin_logs (target_type, target_id, created_at DESC);

