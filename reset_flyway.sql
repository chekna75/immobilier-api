-- Supprimer la table d'historique Flyway
DROP TABLE IF EXISTS flyway_schema_history CASCADE;

-- Supprimer toutes les tables existantes (ATTENTION: cela supprime toutes les données!)
DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;
