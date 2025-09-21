-- Ajouter les colonnes manquantes à la table admin_logs
-- pour correspondre à l'entité AdminLogEntity

ALTER TABLE admin_logs 
ADD COLUMN IF NOT EXISTS ip VARCHAR(45),
ADD COLUMN IF NOT EXISTS user_agent TEXT;

-- Mettre à jour les contraintes pour correspondre à l'entité
ALTER TABLE admin_logs 
ALTER COLUMN admin_id DROP NOT NULL,
ALTER COLUMN target_type DROP NOT NULL,
ALTER COLUMN target_id DROP NOT NULL;
