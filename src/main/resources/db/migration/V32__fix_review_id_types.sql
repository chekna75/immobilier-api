-- Migration pour corriger les types d'ID des tables reviews et review_interactions
-- Les entités Java utilisent maintenant UUID au lieu de Long

-- Supprimer les contraintes de clé étrangère existantes
ALTER TABLE review_interactions DROP CONSTRAINT IF EXISTS FK4kwovr6c3r2khewhp52iex1tx;
ALTER TABLE review_interactions DROP CONSTRAINT IF EXISTS fk_review_interactions_review_id;

-- Supprimer les tables si elles existent avec de mauvais types
DROP TABLE IF EXISTS review_interactions CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;

-- Recréer la table reviews avec UUID
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

-- Recréer la table review_interactions avec UUID
CREATE TABLE review_interactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  review_id UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  interaction_type VARCHAR(20) NOT NULL CHECK (interaction_type IN ('helpful', 'report')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(review_id, user_id, interaction_type) -- Un utilisateur ne peut interagir qu'une fois par type
);

-- Recréer les index
CREATE INDEX idx_reviews_target ON reviews (target_id, target_type);
CREATE INDEX idx_reviews_reviewer ON reviews (reviewer_id);
CREATE INDEX idx_reviews_status ON reviews (status);
CREATE INDEX idx_reviews_created_at ON reviews (created_at DESC);

CREATE INDEX idx_review_interactions_review ON review_interactions (review_id);
CREATE INDEX idx_review_interactions_user ON review_interactions (user_id);

-- Contraintes d'unicité
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
COMMENT ON TABLE review_interactions IS 'Table des interactions utilisateurs avec les avis (utile, signalement)';

COMMENT ON COLUMN reviews.target_id IS 'ID de la cible (bien, propriétaire ou agence)';
COMMENT ON COLUMN reviews.target_type IS 'Type de cible: property, owner, agency';
COMMENT ON COLUMN reviews.ratings IS 'JSON contenant les notes détaillées par critère';
COMMENT ON COLUMN reviews.status IS 'Statut de modération: pending, approved, rejected';

