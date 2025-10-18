-- Ajout des champs enrichis aux listings
ALTER TABLE listings ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 8);
ALTER TABLE listings ADD COLUMN IF NOT EXISTS longitude DECIMAL(11, 8);
ALTER TABLE listings ADD COLUMN IF NOT EXISTS rooms INTEGER;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS floor INTEGER;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS building_year INTEGER;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS energy_class VARCHAR(10);
ALTER TABLE listings ADD COLUMN IF NOT EXISTS has_elevator BOOLEAN DEFAULT FALSE;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS has_parking BOOLEAN DEFAULT FALSE;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS has_balcony BOOLEAN DEFAULT FALSE;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS has_terrace BOOLEAN DEFAULT FALSE;

-- Mise à jour du status pour supporter DRAFT, PUBLISHED, ARCHIVED
ALTER TABLE listings ALTER COLUMN status TYPE VARCHAR(20);
UPDATE listings SET status = 'PUBLISHED' WHERE status = 'ACTIVE';
UPDATE listings SET status = 'ARCHIVED' WHERE status = 'INACTIVE';

-- Ajout d'index pour la recherche géographique
CREATE INDEX IF NOT EXISTS idx_listings_location ON listings(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_listings_status ON listings(status);
CREATE INDEX IF NOT EXISTS idx_listings_rooms ON listings(rooms);

