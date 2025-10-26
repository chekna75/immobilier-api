-- Table pour stocker les informations sur les images uploadées
CREATE TABLE IF NOT EXISTS uploaded_images (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT,
    s3_key VARCHAR(500) NOT NULL,
    public_url VARCHAR(500) NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_uploaded_images_user_id ON uploaded_images(user_id);
CREATE INDEX IF NOT EXISTS idx_uploaded_images_file_name ON uploaded_images(file_name);
CREATE INDEX IF NOT EXISTS idx_uploaded_images_is_used ON uploaded_images(is_used);
CREATE INDEX IF NOT EXISTS idx_uploaded_images_created_at ON uploaded_images(created_at);

-- Contrainte unique pour éviter les doublons
CREATE UNIQUE INDEX IF NOT EXISTS idx_uploaded_images_user_file ON uploaded_images(user_id, file_name);
