-- Migration pour les tables de notifications
-- Version: V20

-- Table pour stocker les device tokens FCM
CREATE TABLE device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(1000) NOT NULL,
    platform VARCHAR(20) NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    app_version VARCHAR(50),
    device_model VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Contraintes
    UNIQUE(token, user_id, platform),
    CONSTRAINT chk_token_length CHECK (LENGTH(token) > 0)
);

-- Index pour optimiser les requêtes
CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);
CREATE INDEX idx_device_tokens_active ON device_tokens(is_active) WHERE is_active = true;
CREATE INDEX idx_device_tokens_platform ON device_tokens(platform);
CREATE INDEX idx_device_tokens_last_used ON device_tokens(last_used_at);

-- Table pour stocker les notifications
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL CHECK (type IN (
        'NEW_LISTING_MATCH', 'NEW_MESSAGE', 'LISTING_STATUS_CHANGE', 
        'PAYMENT_REMINDER', 'SYSTEM_ANNOUNCEMENT', 'FAVORITE_UPDATE', 
        'CONVERSATION_UPDATE'
    )),
    title VARCHAR(255) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    data TEXT, -- JSON string pour les données additionnelles
    is_read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    clicked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    related_entity_type VARCHAR(50), -- "listing", "message", "conversation", etc.
    related_entity_id VARCHAR(100),
    
    -- Contraintes
    CONSTRAINT chk_title_length CHECK (LENGTH(title) > 0),
    CONSTRAINT chk_body_length CHECK (LENGTH(body) > 0)
);

-- Index pour optimiser les requêtes
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = false;
CREATE INDEX idx_notifications_related_entity ON notifications(related_entity_type, related_entity_id);

-- Trigger pour mettre à jour updated_at automatiquement
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_device_tokens_updated_at 
    BEFORE UPDATE ON device_tokens 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Commentaires pour la documentation
COMMENT ON TABLE device_tokens IS 'Stockage des tokens FCM pour les notifications push';
COMMENT ON TABLE notifications IS 'Historique des notifications envoyées aux utilisateurs';

COMMENT ON COLUMN device_tokens.token IS 'Token FCM unique pour chaque appareil';
COMMENT ON COLUMN device_tokens.platform IS 'Plateforme de l''appareil (ANDROID, IOS, WEB)';
COMMENT ON COLUMN device_tokens.is_active IS 'Indique si le token est actif et peut recevoir des notifications';

COMMENT ON COLUMN notifications.type IS 'Type de notification selon NotificationType enum';
COMMENT ON COLUMN notifications.data IS 'Données additionnelles au format JSON';
COMMENT ON COLUMN notifications.related_entity_type IS 'Type d''entité liée (listing, message, etc.)';
COMMENT ON COLUMN notifications.related_entity_id IS 'ID de l''entité liée';

