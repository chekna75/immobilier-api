-- Ajout du support des miniatures pour les images uploadées
ALTER TABLE uploaded_images 
ADD COLUMN thumbnail_s3_key VARCHAR(1000),
ADD COLUMN thumbnail_public_url VARCHAR(1000),
ADD COLUMN thumbnail_generated BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN image_width INTEGER,
ADD COLUMN image_height INTEGER;

-- Index pour améliorer les performances des requêtes sur les miniatures
CREATE INDEX idx_uploaded_images_thumbnail_generated ON uploaded_images(thumbnail_generated);
CREATE INDEX idx_uploaded_images_created_at_is_used ON uploaded_images(created_at, is_used);
