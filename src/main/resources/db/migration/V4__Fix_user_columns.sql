-- Migration pour corriger les colonnes users avec des valeurs NULL
-- V4__Fix_user_columns.sql

-- Mettre à jour les valeurs NULL pour emailVerified
UPDATE users SET emailVerified = false WHERE emailVerified IS NULL;

-- Mettre à jour les valeurs NULL pour phoneVerified  
UPDATE users SET phoneVerified = false WHERE phoneVerified IS NULL;

-- Maintenant on peut ajouter les contraintes NOT NULL
ALTER TABLE users ALTER COLUMN emailVerified SET NOT NULL;
ALTER TABLE users ALTER COLUMN phoneVerified SET NOT NULL;
