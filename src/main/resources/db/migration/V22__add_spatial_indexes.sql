-- Migration pour ajouter des index géospatiaux optimisés
-- Améliore les performances des requêtes de recherche par proximité

-- Index composite sur latitude et longitude pour les requêtes géospatiales
CREATE INDEX IF NOT EXISTS idx_listings_location ON listings (latitude, longitude);

-- Index composite sur status, latitude et longitude pour les requêtes filtrées
CREATE INDEX IF NOT EXISTS idx_listings_status_location ON listings (status, latitude, longitude);

-- Index sur les coordonnées non-null pour éviter les propriétés sans géolocalisation
CREATE INDEX IF NOT EXISTS idx_listings_coords_not_null ON listings (latitude, longitude) 
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Index composite pour les requêtes de proximité avec filtres de prix
CREATE INDEX IF NOT EXISTS idx_listings_proximity_price ON listings (status, latitude, longitude, price)
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Index pour les requêtes de proximité avec filtres de type
CREATE INDEX IF NOT EXISTS idx_listings_proximity_type ON listings (status, latitude, longitude, type)
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Commentaire explicatif
COMMENT ON INDEX idx_listings_location IS 'Index géospatial pour les calculs de distance avec la formule de Haversine';
COMMENT ON INDEX idx_listings_status_location IS 'Index optimisé pour les requêtes de proximité sur les annonces publiées';
COMMENT ON INDEX idx_listings_coords_not_null IS 'Index sur les propriétés avec coordonnées valides uniquement';
COMMENT ON INDEX idx_listings_proximity_price IS 'Index pour les recherches de proximité avec filtres de prix';
COMMENT ON INDEX idx_listings_proximity_type IS 'Index pour les recherches de proximité avec filtres de type';
