-- Augmenter la taille des colonnes URL pour supporter les URLs S3 longues
-- Les URLs S3 peuvent être très longues avec les paramètres de signature

-- Table uploaded_images
ALTER TABLE uploaded_images ALTER COLUMN public_url TYPE VARCHAR(1000);
ALTER TABLE uploaded_images ALTER COLUMN s3_key TYPE VARCHAR(1000);

-- Table listing_photos  
ALTER TABLE listing_photos ALTER COLUMN url TYPE VARCHAR(1000);

