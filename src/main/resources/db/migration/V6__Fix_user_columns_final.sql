-- Migration finale pour corriger les colonnes users
-- V6__Fix_user_columns_final.sql

-- Supprimer les colonnes problématiques si elles existent
ALTER TABLE users DROP COLUMN IF EXISTS emailVerified;
ALTER TABLE users DROP COLUMN IF EXISTS phoneVerified;

-- Recréer les colonnes avec des valeurs par défaut
ALTER TABLE users ADD COLUMN emailVerified boolean DEFAULT false;
ALTER TABLE users ADD COLUMN phoneVerified boolean DEFAULT false;

-- Mettre à jour toutes les lignes existantes
UPDATE users SET emailVerified = false WHERE emailVerified IS NULL;
UPDATE users SET phoneVerified = false WHERE phoneVerified IS NULL;

-- Maintenant on peut ajouter les contraintes NOT NULL
ALTER TABLE users ALTER COLUMN emailVerified SET NOT NULL;
ALTER TABLE users ALTER COLUMN phoneVerified SET NOT NULL;
