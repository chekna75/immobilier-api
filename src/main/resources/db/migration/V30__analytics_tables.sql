-- Types ENUM pour les analytics
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'click_action') THEN
    CREATE TYPE click_action AS ENUM ('VIEW', 'CONTACT', 'FAVORITE', 'SHARE', 'CALL', 'EMAIL', 'VISIT', 'BOOK');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'favorite_action') THEN
    CREATE TYPE favorite_action AS ENUM ('ADD', 'REMOVE');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'contact_type') THEN
    CREATE TYPE contact_type AS ENUM ('MESSAGE', 'CALL', 'EMAIL', 'VISIT', 'BOOKING');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'conversion_type') THEN
    CREATE TYPE conversion_type AS ENUM ('BOOKING', 'RENTAL', 'SALE', 'VISIT', 'INQUIRY');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'conversion_status') THEN
    CREATE TYPE conversion_status AS ENUM ('PENDING', 'COMPLETED', 'CANCELLED', 'FAILED');
  END IF;
END$$;

-- Table pour les vues d'annonces
CREATE TABLE analytics_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    source VARCHAR(50),
    device VARCHAR(50),
    location VARCHAR(100),
    user_agent TEXT,
    session_id VARCHAR(100),
    referrer VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table pour les clics sur les annonces
CREATE TABLE analytics_clicks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action click_action NOT NULL,
    source VARCHAR(50),
    device VARCHAR(50),
    location VARCHAR(100),
    session_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table pour les favoris
CREATE TABLE analytics_favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action favorite_action NOT NULL,
    source VARCHAR(50),
    device VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table pour les contacts
CREATE TABLE analytics_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_type contact_type NOT NULL,
    source VARCHAR(50),
    device VARCHAR(50),
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table pour les recherches
CREATE TABLE analytics_searches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    query VARCHAR(500),
    filters JSONB,
    source VARCHAR(50),
    device VARCHAR(50),
    location VARCHAR(100),
    results_count INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table pour les conversions
CREATE TABLE analytics_conversions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversion_type conversion_type NOT NULL,
    source VARCHAR(50),
    device VARCHAR(50),
    value DECIMAL(10,2),
    status conversion_status NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index pour optimiser les requêtes analytics
CREATE INDEX idx_analytics_views_listing ON analytics_views(listing_id);
CREATE INDEX idx_analytics_views_user ON analytics_views(user_id);
CREATE INDEX idx_analytics_views_created_at ON analytics_views(created_at DESC);
CREATE INDEX idx_analytics_views_source ON analytics_views(source);
CREATE INDEX idx_analytics_views_device ON analytics_views(device);

CREATE INDEX idx_analytics_clicks_listing ON analytics_clicks(listing_id);
CREATE INDEX idx_analytics_clicks_user ON analytics_clicks(user_id);
CREATE INDEX idx_analytics_clicks_action ON analytics_clicks(action);
CREATE INDEX idx_analytics_clicks_created_at ON analytics_clicks(created_at DESC);
CREATE INDEX idx_analytics_clicks_source ON analytics_clicks(source);

CREATE INDEX idx_analytics_favorites_listing ON analytics_favorites(listing_id);
CREATE INDEX idx_analytics_favorites_user ON analytics_favorites(user_id);
CREATE INDEX idx_analytics_favorites_action ON analytics_favorites(action);
CREATE INDEX idx_analytics_favorites_created_at ON analytics_favorites(created_at DESC);

CREATE INDEX idx_analytics_contacts_listing ON analytics_contacts(listing_id);
CREATE INDEX idx_analytics_contacts_user ON analytics_contacts(user_id);
CREATE INDEX idx_analytics_contacts_type ON analytics_contacts(contact_type);
CREATE INDEX idx_analytics_contacts_created_at ON analytics_contacts(created_at DESC);

CREATE INDEX idx_analytics_searches_user ON analytics_searches(user_id);
CREATE INDEX idx_analytics_searches_query ON analytics_searches(query);
CREATE INDEX idx_analytics_searches_created_at ON analytics_searches(created_at DESC);
CREATE INDEX idx_analytics_searches_source ON analytics_searches(source);

CREATE INDEX idx_analytics_conversions_listing ON analytics_conversions(listing_id);
CREATE INDEX idx_analytics_conversions_user ON analytics_conversions(user_id);
CREATE INDEX idx_analytics_conversions_type ON analytics_conversions(conversion_type);
CREATE INDEX idx_analytics_conversions_status ON analytics_conversions(status);
CREATE INDEX idx_analytics_conversions_created_at ON analytics_conversions(created_at DESC);

-- Index composites pour les requêtes fréquentes
CREATE INDEX idx_analytics_views_listing_date ON analytics_views(listing_id, created_at DESC);
CREATE INDEX idx_analytics_views_user_date ON analytics_views(user_id, created_at DESC);
CREATE INDEX idx_analytics_clicks_listing_date ON analytics_clicks(listing_id, created_at DESC);
CREATE INDEX idx_analytics_favorites_listing_date ON analytics_favorites(listing_id, created_at DESC);
CREATE INDEX idx_analytics_contacts_listing_date ON analytics_contacts(listing_id, created_at DESC);
CREATE INDEX idx_analytics_conversions_listing_date ON analytics_conversions(listing_id, created_at DESC);

-- Commentaires pour la documentation
COMMENT ON TABLE analytics_views IS 'Suivi des vues d''annonces pour les analytics';
COMMENT ON TABLE analytics_clicks IS 'Suivi des clics sur les annonces pour les analytics';
COMMENT ON TABLE analytics_favorites IS 'Suivi des ajouts/suppressions de favoris pour les analytics';
COMMENT ON TABLE analytics_contacts IS 'Suivi des contacts avec les propriétaires pour les analytics';
COMMENT ON TABLE analytics_searches IS 'Suivi des recherches d''utilisateurs pour les analytics';
COMMENT ON TABLE analytics_conversions IS 'Suivi des conversions (réservations, locations) pour les analytics';
