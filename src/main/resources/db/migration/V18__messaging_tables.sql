-- Migration pour les tables de messagerie
-- V18__messaging_tables.sql

-- Table des conversations
CREATE TABLE conversations (
  id                    BIGSERIAL PRIMARY KEY,
  property_id           UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
  tenant_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  owner_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  
  -- Métadonnées de conversation
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  is_archived           BOOLEAN NOT NULL DEFAULT FALSE,
  last_message          TEXT,
  last_message_time     TIMESTAMPTZ,
  
  -- Compteurs de messages non lus
  tenant_unread_count   INTEGER NOT NULL DEFAULT 0,
  owner_unread_count    INTEGER NOT NULL DEFAULT 0,
  
  -- Timestamps
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  -- Contrainte: une seule conversation active par propriété et paire d'utilisateurs
  CONSTRAINT uq_conversation_property_users UNIQUE (property_id, tenant_id, owner_id, is_active)
);

-- Table des messages
CREATE TABLE messages (
  id                    BIGSERIAL PRIMARY KEY,
  conversation_id       BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  sender_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  
  -- Contenu du message
  content               TEXT NOT NULL,
  message_type          VARCHAR(20) NOT NULL DEFAULT 'TEXT',
  
  -- Statut de lecture
  is_read               BOOLEAN NOT NULL DEFAULT FALSE,
  read_at               TIMESTAMPTZ,
  
  -- Timestamps
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index pour les performances
CREATE INDEX idx_conversations_tenant ON conversations (tenant_id, is_active, last_message_time DESC);
CREATE INDEX idx_conversations_owner ON conversations (owner_id, is_active, last_message_time DESC);
CREATE INDEX idx_conversations_property ON conversations (property_id, is_active);

CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at ASC);
CREATE INDEX idx_messages_sender ON messages (sender_id, created_at DESC);
CREATE INDEX idx_messages_unread ON messages (conversation_id, is_read) WHERE is_read = FALSE;

-- Trigger pour mettre à jour updated_at sur conversations
CREATE TRIGGER trg_conversations_updated_at
  BEFORE UPDATE ON conversations
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- Fonction pour incrémenter le compteur de messages non lus
CREATE OR REPLACE FUNCTION increment_unread_count()
RETURNS TRIGGER AS $$
BEGIN
  -- Incrémenter le compteur pour l'autre utilisateur (pas l'expéditeur)
  IF NEW.sender_id = (SELECT tenant_id FROM conversations WHERE id = NEW.conversation_id) THEN
    -- Le locataire a envoyé, incrémenter pour le propriétaire
    UPDATE conversations 
    SET owner_unread_count = owner_unread_count + 1,
        last_message = NEW.content,
        last_message_time = NEW.created_at
    WHERE id = NEW.conversation_id;
  ELSE
    -- Le propriétaire a envoyé, incrémenter pour le locataire
    UPDATE conversations 
    SET tenant_unread_count = tenant_unread_count + 1,
        last_message = NEW.content,
        last_message_time = NEW.created_at
    WHERE id = NEW.conversation_id;
  END IF;
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger pour incrémenter automatiquement les compteurs
CREATE TRIGGER trg_messages_increment_unread
  AFTER INSERT ON messages
  FOR EACH ROW EXECUTE FUNCTION increment_unread_count();

-- Fonction pour réinitialiser les compteurs de messages non lus
CREATE OR REPLACE FUNCTION reset_unread_count(conversation_id BIGINT, user_id UUID)
RETURNS VOID AS $$
BEGIN
  -- Marquer tous les messages comme lus pour cet utilisateur
  UPDATE messages 
  SET is_read = TRUE, read_at = NOW()
  WHERE conversation_id = $1 
    AND sender_id != $2 
    AND is_read = FALSE;
  
  -- Réinitialiser le compteur approprié
  IF (SELECT tenant_id FROM conversations WHERE id = $1) = $2 THEN
    UPDATE conversations SET tenant_unread_count = 0 WHERE id = $1;
  ELSE
    UPDATE conversations SET owner_unread_count = 0 WHERE id = $1;
  END IF;
END;
$$ LANGUAGE plpgsql;
