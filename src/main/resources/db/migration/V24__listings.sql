-- Types ENUM
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'listing_status') THEN
    CREATE TYPE listing_status AS ENUM ('ACTIVE','REMOVED');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'listing_type') THEN
    CREATE TYPE listing_type AS ENUM ('RENT','SALE');
  END IF;
END$$;

-- Table des annonces
CREATE TABLE IF NOT EXISTS listings (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id     UUID NOT NULL REFERENCES users(id),
  status       listing_status NOT NULL DEFAULT 'ACTIVE',
  type         listing_type NOT NULL,
  city         VARCHAR(100) NOT NULL,
  district     VARCHAR(100),
  price        NUMERIC NOT NULL,
  title        VARCHAR(255) NOT NULL,
  description  TEXT,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table des photos d'annonces (max 5 par annonce : contrainte dans l'application)
CREATE TABLE IF NOT EXISTS listing_photos (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
  url        TEXT NOT NULL,
  ordering   INT NOT NULL
);

-- Index pour les filtrages fréquents
CREATE INDEX IF NOT EXISTS idx_listings_city ON listings (LOWER(city));
CREATE INDEX IF NOT EXISTS idx_listings_type ON listings (type);
