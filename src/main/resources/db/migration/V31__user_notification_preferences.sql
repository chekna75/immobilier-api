-- Migration pour la table des préférences de notifications utilisateur
-- Version: V21

-- Table pour stocker les préférences de notifications par utilisateur
CREATE TABLE user_notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Préférences générales
    payment_reminders BOOLEAN NOT NULL DEFAULT true,
    overdue_alerts BOOLEAN NOT NULL DEFAULT true,
    payment_confirmations BOOLEAN NOT NULL DEFAULT true,
    new_contracts BOOLEAN NOT NULL DEFAULT true,
    new_messages BOOLEAN NOT NULL DEFAULT true,
    listing_status_changes BOOLEAN NOT NULL DEFAULT true,
    favorite_updates BOOLEAN NOT NULL DEFAULT true,
    system_updates BOOLEAN NOT NULL DEFAULT true,
    marketing_notifications BOOLEAN NOT NULL DEFAULT false,
    
    -- Préférences de rappels de paiement
    reminder_days INTEGER[] NOT NULL DEFAULT '{1,3,7}', -- Jours avant échéance
    reminder_time TIME NOT NULL DEFAULT '09:00',
    overdue_frequency VARCHAR(20) NOT NULL DEFAULT 'daily', -- daily, weekly
    
    -- Préférences de canaux
    push_enabled BOOLEAN NOT NULL DEFAULT true,
    email_enabled BOOLEAN NOT NULL DEFAULT true,
    sms_enabled BOOLEAN NOT NULL DEFAULT false,
    
    -- Heures silencieuses
    quiet_hours_enabled BOOLEAN NOT NULL DEFAULT false,
    quiet_hours_start TIME NOT NULL DEFAULT '22:00',
    quiet_hours_end TIME NOT NULL DEFAULT '08:00',
    
    -- Fréquence des notifications
    notification_frequency VARCHAR(20) NOT NULL DEFAULT 'immediate', -- immediate, daily, weekly
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Contraintes
    CONSTRAINT chk_reminder_days CHECK (array_length(reminder_days, 1) > 0),
    CONSTRAINT chk_overdue_frequency CHECK (overdue_frequency IN ('daily', 'weekly')),
    CONSTRAINT chk_notification_frequency CHECK (notification_frequency IN ('immediate', 'daily', 'weekly')),
    CONSTRAINT chk_quiet_hours CHECK (quiet_hours_start != quiet_hours_end)
);

-- Index pour optimiser les requêtes
CREATE INDEX idx_user_notification_preferences_user_id ON user_notification_preferences(user_id);
CREATE UNIQUE INDEX idx_user_notification_preferences_user_unique ON user_notification_preferences(user_id);

-- Trigger pour mettre à jour updated_at automatiquement
CREATE TRIGGER update_user_notification_preferences_updated_at 
    BEFORE UPDATE ON user_notification_preferences 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Commentaires pour la documentation
COMMENT ON TABLE user_notification_preferences IS 'Préférences de notifications par utilisateur';
COMMENT ON COLUMN user_notification_preferences.reminder_days IS 'Jours avant échéance pour les rappels (ex: {1,3,7})';
COMMENT ON COLUMN user_notification_preferences.quiet_hours_start IS 'Heure de début des heures silencieuses';
COMMENT ON COLUMN user_notification_preferences.quiet_hours_end IS 'Heure de fin des heures silencieuses';
COMMENT ON COLUMN user_notification_preferences.notification_frequency IS 'Fréquence d''envoi des notifications groupées';
