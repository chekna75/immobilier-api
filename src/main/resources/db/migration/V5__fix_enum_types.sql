-- Convertir les types enum PostgreSQL en VARCHAR pour éviter les problèmes de mapping Hibernate

-- Supprimer d'abord les valeurs par défaut qui dépendent des types enum
ALTER TABLE listings ALTER COLUMN status DROP DEFAULT;
ALTER TABLE listings ALTER COLUMN type DROP DEFAULT;

-- Convertir les colonnes en VARCHAR
ALTER TABLE listings 
ALTER COLUMN status TYPE VARCHAR(20) USING status::text,
ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

-- Supprimer les types enum personnalisés avec CASCADE pour forcer la suppression
DROP TYPE IF EXISTS listing_status CASCADE;
DROP TYPE IF EXISTS listing_type CASCADE;
