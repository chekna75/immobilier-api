-- Migration pour les fonctionnalités sociales
-- Création des tables pour les avis et partages sociaux

-- Table des avis et évaluations
CREATE TABLE reviews (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  target_id UUID NOT NULL, -- ID du bien/propriétaire/agence
  target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('property', 'owner', 'agency')),
  reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  overall_rating DECIMAL(2,1) NOT NULL CHECK (overall_rating >= 1 AND overall_rating <= 5),
  title VARCHAR(255),
  comment TEXT,
  ratings JSONB NOT NULL, -- Critères détaillés avec poids
  status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
  helpful_count INTEGER DEFAULT 0,
  report_count INTEGER DEFAULT 0,
  is_verified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table pour tracker les partages sociaux
CREATE TABLE social_shares (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id UUID REFERENCES listings(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  platform VARCHAR(50) NOT NULL CHECK (platform IN ('facebook', 'twitter', 'instagram', 'linkedin', 'whatsapp', 'telegram', 'native')),
  share_type VARCHAR(20) NOT NULL CHECK (share_type IN ('property', 'favorite', 'search')),
  shared_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  metadata JSONB -- Données additionnelles (message personnalisé, etc.)
);

-- Table pour les interactions avec les avis (utile, signalement)
CREATE TABLE review_interactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  review_id UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  interaction_type VARCHAR(20) NOT NULL CHECK (interaction_type IN ('helpful', 'report')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(review_id, user_id, interaction_type) -- Un utilisateur ne peut interagir qu'une fois par type
);

-- Index pour les performances
CREATE INDEX idx_reviews_target ON reviews (target_id, target_type);
CREATE INDEX idx_reviews_reviewer ON reviews (reviewer_id);
CREATE INDEX idx_reviews_status ON reviews (status);
CREATE INDEX idx_reviews_created_at ON reviews (created_at DESC);

CREATE INDEX idx_social_shares_listing ON social_shares (listing_id);
CREATE INDEX idx_social_shares_user ON social_shares (user_id);
CREATE INDEX idx_social_shares_platform ON social_shares (platform);
CREATE INDEX idx_social_shares_shared_at ON social_shares (shared_at DESC);

CREATE INDEX idx_review_interactions_review ON review_interactions (review_id);
CREATE INDEX idx_review_interactions_user ON review_interactions (user_id);

-- Contraintes d'unicité
-- Un utilisateur ne peut laisser qu'un seul avis par cible
ALTER TABLE reviews ADD CONSTRAINT unique_review_per_target 
  UNIQUE (target_id, target_type, reviewer_id);

-- Triggers pour updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_reviews_updated_at 
  BEFORE UPDATE ON reviews 
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Commentaires pour documentation
COMMENT ON TABLE reviews IS 'Table des avis et évaluations des biens, propriétaires et agences';
COMMENT ON TABLE social_shares IS 'Table de tracking des partages sur les réseaux sociaux';
COMMENT ON TABLE review_interactions IS 'Table des interactions utilisateurs avec les avis (utile, signalement)';

COMMENT ON COLUMN reviews.target_id IS 'ID de la cible (bien, propriétaire ou agence)';
COMMENT ON COLUMN reviews.target_type IS 'Type de cible: property, owner, agency';
COMMENT ON COLUMN reviews.ratings IS 'JSON contenant les notes détaillées par critère';
COMMENT ON COLUMN reviews.status IS 'Statut de modération: pending, approved, rejected';

COMMENT ON COLUMN social_shares.platform IS 'Plateforme de partage utilisée';
COMMENT ON COLUMN social_shares.share_type IS 'Type de contenu partagé';
COMMENT ON COLUMN social_shares.metadata IS 'Données additionnelles du partage';
