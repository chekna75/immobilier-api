-- V1__init.sql

-- Extension utile pour UUID (si nécessaire)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enum simples
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
    CREATE TYPE user_role AS ENUM ('TENANT', 'OWNER', 'ADMIN');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN
    CREATE TYPE user_status AS ENUM ('ACTIVE', 'SUSPENDED');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'otp_channel') THEN
    CREATE TYPE otp_channel AS ENUM ('SMS', 'EMAIL');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'otp_purpose') THEN
    CREATE TYPE otp_purpose AS ENUM ('LOGIN', 'VERIFY_PHONE', 'RESET_PASSWORD');
  END IF;
END$$;

-- Utilisateurs
CREATE TABLE users (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  role              user_role NOT NULL DEFAULT 'TENANT',
  status            user_status NOT NULL DEFAULT 'ACTIVE',

  email             VARCHAR(255),
  phone_e164        VARCHAR(20),
  phone_verified    BOOLEAN NOT NULL DEFAULT FALSE,
  email_verified    BOOLEAN NOT NULL DEFAULT FALSE,

  password_hash     VARCHAR(255),
  first_name        VARCHAR(80),
  last_name         VARCHAR(80),
  avatar_url        TEXT,

  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  -- Au moins email OU téléphone
  CONSTRAINT chk_contact CHECK ((email IS NOT NULL) OR (phone_e164 IS NOT NULL))
);

-- Contrainte: si email est défini, password_hash doit être défini (login par email)
CREATE OR REPLACE FUNCTION ensure_password_when_email()
RETURNS TRIGGER AS $$
BEGIN
  IF (NEW.email IS NOT NULL AND (NEW.password_hash IS NULL OR length(NEW.password_hash) = 0)) THEN
    RAISE EXCEPTION 'password_hash is required when email is set';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_password_when_email
BEFORE INSERT OR UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION ensure_password_when_email();

-- Unicités
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email ON users (LOWER(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_phone ON users (phone_e164)   WHERE phone_e164 IS NOT NULL;

-- OTP
CREATE TABLE otp_codes (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  code            VARCHAR(10) NOT NULL,
  channel         otp_channel NOT NULL,
  purpose         otp_purpose NOT NULL,
  expires_at      TIMESTAMPTZ NOT NULL,
  used_at         TIMESTAMPTZ,
  attempt_count   INT NOT NULL DEFAULT 0,
  meta            JSONB,

  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_otp_lookup ON otp_codes (user_id, purpose) WHERE used_at IS NULL;

-- Tokens de rafraîchissement (sessions persistantes)
CREATE TABLE refresh_tokens (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash      VARCHAR(255) NOT NULL,       -- stocker hash, pas le token brut
  user_agent      TEXT,
  ip_addr         INET,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at      TIMESTAMPTZ NOT NULL,
  revoked_at      TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_refresh_user ON refresh_tokens (user_id);

-- Logs admin (suspensions, etc.) - utile dès P0 Admin minimal
CREATE TABLE admin_logs (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  admin_id        UUID NOT NULL REFERENCES users(id) ON DELETE SET NULL,
  action          VARCHAR(120) NOT NULL,
  target_type     VARCHAR(60),
  target_id       UUID,
  details         JSONB,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Trigger updated_at
CREATE OR REPLACE FUNCTION touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_touch
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
