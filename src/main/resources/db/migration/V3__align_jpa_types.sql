-- V2__align_jpa_types.sql
-- Convertit les ENUM et types spécifiques vers VARCHAR pour matcher JPA @Enumerated STRING et String

-- ===== otp_codes =====
-- channel/purpose : ENUM -> VARCHAR
ALTER TABLE otp_codes
  ALTER COLUMN channel TYPE VARCHAR(20) USING channel::text,
  ALTER COLUMN purpose TYPE VARCHAR(20) USING purpose::text;

-- code : borne raisonnable
ALTER TABLE otp_codes
  ALTER COLUMN code TYPE VARCHAR(10);

-- ===== refresh_tokens =====
-- ip_addr INET -> VARCHAR(45) (IPv6 max ~39 ; on prend 45 pour marge)
ALTER TABLE refresh_tokens
  ALTER COLUMN ip_addr TYPE VARCHAR(45) USING ip_addr::text;

-- user_agent : borne raisonnable 255
ALTER TABLE refresh_tokens
  ALTER COLUMN user_agent TYPE VARCHAR(255);

-- ===== users =====
-- avatar_url : TEXT -> VARCHAR(255) (si tu veux garder TEXT, voir Option B plus bas)
ALTER TABLE users
  ALTER COLUMN avatar_url TYPE VARCHAR(255);

-- first/last name : borne raisonnable
ALTER TABLE users
  ALTER COLUMN first_name TYPE VARCHAR(80),
  ALTER COLUMN last_name  TYPE VARCHAR(80);

-- phone_e164 : borne 20 (ex: +225xxxxxxxxxx rentre largement)
ALTER TABLE users
  ALTER COLUMN phone_e164 TYPE VARCHAR(20);

-- role/status : ENUM -> VARCHAR
ALTER TABLE users
  ALTER COLUMN role   TYPE VARCHAR(20) USING role::text,
  ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- (Optionnel) si plus aucun champ n'utilise les types ENUM natifs Postgres, tu peux les supprimer :
-- DROP TYPE IF EXISTS user_role;
-- DROP TYPE IF EXISTS user_status;
-- DROP TYPE IF EXISTS otp_channel;
-- DROP TYPE IF EXISTS otp_purpose;
