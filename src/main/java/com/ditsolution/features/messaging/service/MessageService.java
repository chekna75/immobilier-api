package com.ditsolution.features.messaging.service;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.messaging.dto.MessageDto;
import com.ditsolution.features.messaging.dto.SendMessageRequest;
import com.ditsolution.features.messaging.entity.ConversationEntity;
import com.ditsolution.features.messaging.entity.MessageEntity;
import com.ditsolution.features.messaging.repository.ConversationRepository;
import com.ditsolution.features.messaging.repository.MessageRepository;
import com.ditsolution.features.messaging.mapper.MessageMapper;
import com.ditsolution.shared.dto.PagedResponse;
import com.ditsolution.features.notification.service.NotificationTriggerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class MessageService {
    
    @Inject
    MessageRepository messageRepository;
    
    @Inject
    ConversationRepository conversationRepository;
    
    @Inject
    MessageMapper messageMapper;
    
    @Inject
    WebSocketService webSocketService;
    
    @Inject
    NotificationTriggerService notificationTriggerService;
    
    /**
     * Envoie un message dans une conversation
     */
    @Transactional
    public MessageDto sendMessage(Long conversationId, SendMessageRequest request, UserEntity sender) {
        // Vérifier que la conversation existe et que l'utilisateur y a accès
        ConversationEntity conversation = conversationRepository
            .findByIdAndUser(conversationId, sender)
            .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
        
        // Créer le message
        MessageEntity message = new MessageEntity(
            conversation,
            sender,
            request.getContent(),
            MessageEntity.MessageType.valueOf(request.getMessageType())
        );
        
        messageRepository.persist(message);
        
        // Mettre à jour la conversation
        conversation.setLastMessage(request.getContent());
        conversation.setLastMessageTime(LocalDateTime.now());
        
        // Incrémenter le compteur de messages non lus pour l'autre utilisateur
        UserEntity otherUser = conversation.getOtherUser(sender);
        if (otherUser != null) {
            conversation.incrementUnreadCount(otherUser);
        }
        
        conversationRepository.persist(conversation);
        
        MessageDto messageDto = messageMapper.toDto(message);
        
        // Notifier via WebSocket
        webSocketService.notifyNewMessage(messageDto, conversationId);
        
        // Déclencher une notification push
        notificationTriggerService.triggerNewMessageNotification(message);
        
        return messageDto;
    }
    
    /**
     * Récupère les messages d'une conversation
     */
    public PagedResponse<MessageDto> getMessages(Long conversationId, UserEntity user, int page, int size) {
        // Vérifier que la conversation existe et que l'utilisateur y a accès
        ConversationEntity conversation = conversationRepository
            .findByIdAndUser(conversationId, user)
            .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
        
        List<MessageEntity> allMessages = messageRepository
            .findByConversationOrderByCreatedAtAsc(conversation);
        
        // Pagination manuelle
        int start = page * size;
        int end = Math.min(start + size, allMessages.size());
        List<MessageEntity> pagedMessages = allMessages.subList(start, end);
        
        List<MessageDto> messageDtos = pagedMessages
            .stream()
            .map(messageMapper::toDto)
            .collect(Collectors.toList());
        
        // int totalPages = (int) Math.ceil((double) allMessages.size() / size);
        
        return new PagedResponse<MessageDto>(
            messageDtos,
            (long) allMessages.size(),
            page,
            size
        );
    }
    
    /**
     * Récupère les messages récents d'une conversation
     */
    public List<MessageDto> getRecentMessages(Long conversationId, UserEntity user, int limit) {
        ConversationEntity conversation = conversationRepository
            .findByIdAndUser(conversationId, user)
            .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
        
        List<MessageEntity> messages = messageRepository
            .findRecentByConversation(conversation, limit);
        
        return messages.stream()
            .map(messageMapper::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Marque les messages d'une conversation comme lus
     */
    @Transactional
    public void markMessagesAsRead(Long conversationId, UserEntity user) {
        ConversationEntity conversation = conversationRepository
            .findByIdAndUser(conversationId, user)
            .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
        
        // Marquer les messages comme lus
        messageRepository.markMessagesAsReadInConversation(conversation, user, LocalDateTime.now());
        
        // Réinitialiser le compteur de messages non lus
        conversation.resetUnreadCount(user);
        conversationRepository.persist(conversation);
        
        // Notifier via WebSocket que les messages ont été lus
        webSocketService.notifyMessageRead(conversationId, user.id, null);
    }
    
    /**
     * Récupère le nombre de messages non lus dans une conversation
     */
    public Long getUnreadCountInConversation(Long conversationId, UserEntity user) {
        ConversationEntity conversation = conversationRepository
            .findByIdAndUser(conversationId, user)
            .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
        
        return messageRepository.countUnreadMessagesInConversation(conversation, user);
    }
    
    /**
     * Récupère tous les messages non lus d'un utilisateur
     */
    public List<MessageDto> getUnreadMessages(UserEntity user) {
        List<MessageEntity> messages = messageRepository.findUnreadMessagesForUser(user);
        
        return messages.stream()
            .map(messageMapper::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Récupère le nombre total de messages non lus pour un utilisateur
     */
    public Long getTotalUnreadCount(UserEntity user) {
        return messageRepository.countTotalUnreadMessagesForUser(user);
    }
    
    /**
     * Récupère les messages d'une conversation après une date donnée
     */
    public List<MessageDto> getMessagesAfterDate(Long conversationId, UserEntity user, LocalDateTime since) {
        ConversationEntity conversation = conversationRepository
            .findByIdAndUser(conversationId, user)
            .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
        
        List<MessageEntity> messages = messageRepository
            .findMessagesAfterDate(conversation, since);
        
        return messages.stream()
            .map(messageMapper::toDto)
            .collect(Collectors.toList());
    }
    
    // ===== MÉTHODES D'ADMINISTRATION =====
    
    /**
     * Récupère tous les messages d'une conversation pour les administrateurs
     */
    public PagedResponse<MessageDto> getMessagesForAdmin(Long conversationId, int page, int size) {
        ConversationEntity conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation non trouvée");
        }
        
        List<MessageEntity> allMessages = messageRepository
            .findByConversationOrderByCreatedAtAsc(conversation);
        
        // Pagination manuelle
        int start = page * size;
        int end = Math.min(start + size, allMessages.size());
        List<MessageEntity> pagedMessages = allMessages.subList(start, end);
        
        List<MessageDto> messageDtos = pagedMessages
            .stream()
            .map(messageMapper::toDto)
            .collect(Collectors.toList());
        
        return new PagedResponse<MessageDto>(
            messageDtos,
            (long) allMessages.size(),
            page,
            size
        );
    }
    
    /**
     * Récupère un message spécifique pour les administrateurs
     */
    public MessageDto getMessageForAdmin(Long conversationId, Long messageId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation non trouvée");
        }
        
        MessageEntity message = messageRepository.findById(messageId);
        if (message == null) {
            throw new RuntimeException("Message non trouvé");
        }
        
        // Vérifier que le message appartient à la conversation
        if (!message.getConversation().getId().equals(conversationId)) {
            throw new RuntimeException("Le message n'appartient pas à cette conversation");
        }
        
        return messageMapper.toDto(message);
    }
    
    /**
     * Supprime un message pour les administrateurs
     */
    @Transactional
    public void deleteMessageForAdmin(Long conversationId, Long messageId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation non trouvée");
        }
        
        MessageEntity message = messageRepository.findById(messageId);
        if (message == null) {
            throw new RuntimeException("Message non trouvé");
        }
        
        // Vérifier que le message appartient à la conversation
        if (!message.getConversation().getId().equals(conversationId)) {
            throw new RuntimeException("Le message n'appartient pas à cette conversation");
        }
        
        messageRepository.delete(message);
        
        // Mettre à jour le dernier message de la conversation si nécessaire
        List<MessageEntity> remainingMessages = messageRepository
            .findByConversationOrderByCreatedAtAsc(conversation);
        
        if (remainingMessages.isEmpty()) {
            conversation.setLastMessage(null);
            conversation.setLastMessageTime(null);
        } else {
            MessageEntity lastMessage = remainingMessages.get(remainingMessages.size() - 1);
            conversation.setLastMessage(lastMessage.getContent());
            conversation.setLastMessageTime(lastMessage.getCreatedAt());
        }
        
        conversationRepository.persist(conversation);
    }
    
    /**
     * Récupère les messages d'une conversation depuis une date donnée pour les administrateurs
     */
    public List<MessageDto> getMessagesSinceForAdmin(Long conversationId, LocalDateTime since) {
        ConversationEntity conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation non trouvée");
        }
        
        List<MessageEntity> messages = messageRepository
            .findMessagesAfterDate(conversation, since);
        
        return messages.stream()
            .map(messageMapper::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Récupère les statistiques des messages d'une conversation pour les administrateurs
     */
    public java.util.Map<String, Object> getMessageStatsForAdmin(Long conversationId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation non trouvée");
        }
        
        long totalMessages = messageRepository.count("conversation = ?1", conversation);
        long readMessages = messageRepository.count("conversation = ?1 AND isRead = true", conversation);
        long unreadMessages = messageRepository.count("conversation = ?1 AND isRead = false", conversation);
        
        return java.util.Map.of(
            "totalMessages", totalMessages,
            "readMessages", readMessages,
            "unreadMessages", unreadMessages
        );
    }
}
