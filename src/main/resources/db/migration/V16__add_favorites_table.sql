-- V16__add_favorites_table.sql
-- Ajout de la table des favoris

CREATE TABLE favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Contrainte d'unicité : un utilisateur ne peut pas avoir le même bien en favori plusieurs fois
    UNIQUE(user_id, listing_id)
);

-- Index pour optimiser les requêtes
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_listing_id ON favorites(listing_id);
CREATE INDEX idx_favorites_created_at ON favorites(created_at);

