-- Migration V7 - Correction définitive des colonnes users
-- Supprimer les colonnes problématiques
ALTER TABLE users DROP COLUMN IF EXISTS emailVerified;
ALTER TABLE users DROP COLUMN IF EXISTS phoneVerified;

-- Recréer les colonnes avec des valeurs par défaut
ALTER TABLE users ADD COLUMN emailVerified boolean DEFAULT false NOT NULL;
ALTER TABLE users ADD COLUMN phoneVerified boolean DEFAULT false NOT NULL;
