package com.ditsolution.features.messaging.repository;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.messaging.entity.ConversationEntity;
import com.ditsolution.features.messaging.entity.MessageEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class MessageRepository implements PanacheRepository<MessageEntity> {
    
    /**
     * Récupère tous les messages d'une conversation
     */
    public List<MessageEntity> findByConversationOrderByCreatedAtAsc(ConversationEntity conversation) {
        return find("conversation = ?1 ORDER BY createdAt ASC", conversation).list();
    }
    
    /**
     * Récupère les messages récents d'une conversation
     */
    public List<MessageEntity> findRecentByConversation(ConversationEntity conversation, int limit) {
        return find("conversation = ?1 ORDER BY createdAt DESC", conversation).page(0, limit).list();
    }
    
    /**
     * Compte le nombre de messages non lus dans une conversation pour un utilisateur
     */
    public Long countUnreadMessagesInConversation(ConversationEntity conversation, UserEntity user) {
        return count("conversation = ?1 AND sender != ?2 AND isRead = false", conversation, user);
    }
    
    /**
     * Marque tous les messages d'une conversation comme lus pour un utilisateur
     */
    public int markMessagesAsReadInConversation(ConversationEntity conversation, UserEntity user, LocalDateTime readAt) {
        return update("isRead = true, readAt = ?1 WHERE conversation = ?2 AND sender != ?3 AND isRead = false", 
                     readAt, conversation, user);
    }
    
    /**
     * Récupère les messages non lus d'un utilisateur dans toutes ses conversations
     */
    public List<MessageEntity> findUnreadMessagesForUser(UserEntity user) {
        return find("conversation.tenant = ?1 OR conversation.owner = ?1 AND sender != ?1 AND isRead = false AND conversation.isActive = true ORDER BY createdAt DESC", user).list();
    }
    
    /**
     * Compte le nombre total de messages non lus pour un utilisateur
     */
    public Long countTotalUnreadMessagesForUser(UserEntity user) {
        return count("conversation.tenant = ?1 OR conversation.owner = ?1 AND sender != ?1 AND isRead = false AND conversation.isActive = true", user);
    }
    
    /**
     * Récupère les messages d'une conversation après une date donnée
     */
    public List<MessageEntity> findMessagesAfterDate(ConversationEntity conversation, LocalDateTime since) {
        return find("conversation = ?1 AND createdAt > ?2 ORDER BY createdAt ASC", conversation, since).list();
    }
    
    /**
     * Récupère les messages d'une conversation entre deux dates
     */
    public List<MessageEntity> findMessagesBetweenDates(ConversationEntity conversation, LocalDateTime startDate, LocalDateTime endDate) {
        return find("conversation = ?1 AND createdAt BETWEEN ?2 AND ?3 ORDER BY createdAt ASC", conversation, startDate, endDate).list();
    }
    
    /**
     * Supprime les messages d'une conversation
     */
    public void deleteByConversation(ConversationEntity conversation) {
        delete("conversation = ?1", conversation);
    }
    
    /**
     * Récupère les messages d'un type spécifique dans une conversation
     */
    public List<MessageEntity> findByConversationAndMessageTypeOrderByCreatedAtDesc(ConversationEntity conversation, MessageEntity.MessageType messageType) {
        return find("conversation = ?1 AND messageType = ?2 ORDER BY createdAt DESC", conversation, messageType).list();
    }
}
