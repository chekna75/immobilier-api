-- Corriger la séquence pour la table uploaded_images
-- Renommer la séquence générée automatiquement par BIGSERIAL si elle existe
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'uploaded_images_id_seq') THEN
        ALTER SEQUENCE uploaded_images_id_seq RENAME TO uploaded_images_seq;
    END IF;
END $$;

