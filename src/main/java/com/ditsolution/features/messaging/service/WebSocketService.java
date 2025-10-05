package com.ditsolution.features.messaging.service;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.messaging.dto.MessageDto;
import com.ditsolution.features.messaging.websocket.MessageWebSocket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class WebSocketService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    
    @Inject
    MessageService messageService;
    
    @Inject
    ConversationService conversationService;
    
    /**
     * Notifie l'envoi d'un nouveau message via WebSocket
     */
    public void notifyNewMessage(MessageDto message, Long conversationId) {
        try {
            // Créer le message WebSocket
            MessageWebSocket.WebSocketMessage wsMessage = new MessageWebSocket.WebSocketMessage();
            wsMessage.setType("message");
            wsMessage.getData().put("id", message.getId());
            wsMessage.getData().put("conversationId", conversationId);
            wsMessage.getData().put("senderId", message.getSender().id().toString());
            wsMessage.getData().put("content", message.getContent());
            wsMessage.getData().put("messageType", message.getMessageType());
            wsMessage.getData().put("timestamp", message.getCreatedAt().toString());
            
            // Envoyer à tous les participants de la conversation
            MessageWebSocket.sendMessageToConversation(conversationId, wsMessage, null);
            
            logger.info("Message WebSocket envoyé pour la conversation: {}", conversationId);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi du message WebSocket", e);
        }
    }
    
    /**
     * Notifie qu'un utilisateur est en train de taper
     */
    public void notifyTyping(Long conversationId, Long userId, Boolean isTyping) {
        try {
            MessageWebSocket.WebSocketMessage wsMessage = new MessageWebSocket.WebSocketMessage();
            wsMessage.setType("typing");
            wsMessage.getData().put("conversationId", conversationId);
            wsMessage.getData().put("userId", userId.toString());
            wsMessage.getData().put("isTyping", isTyping);
            
            // Envoyer à tous les autres participants de la conversation
            MessageWebSocket.sendMessageToConversation(conversationId, wsMessage, null);
            
            logger.debug("Notification de frappe envoyée pour la conversation: {}", conversationId);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification de frappe", e);
        }
    }
    
    /**
     * Notifie qu'un message a été lu
     */
    public void notifyMessageRead(Long conversationId, Object userId, Long messageId) {
        try {
            MessageWebSocket.WebSocketMessage wsMessage = new MessageWebSocket.WebSocketMessage();
            wsMessage.setType("message_read");
            wsMessage.getData().put("conversationId", conversationId);
            wsMessage.getData().put("userId", userId.toString());
            wsMessage.getData().put("messageId", messageId);
            
            // Envoyer à tous les autres participants de la conversation
            MessageWebSocket.sendMessageToConversation(conversationId, wsMessage, null);
            
            logger.debug("Notification de lecture envoyée pour la conversation: {}", conversationId);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification de lecture", e);
        }
    }
    
    /**
     * Notifie qu'un utilisateur est en ligne
     */
    public void notifyUserOnline(java.util.UUID userId) {
        try {
            MessageWebSocket.WebSocketMessage wsMessage = new MessageWebSocket.WebSocketMessage();
            wsMessage.setType("user_online");
            wsMessage.getData().put("userId", userId.toString());
            wsMessage.getData().put("isOnline", true);
            
            // Envoyer à tous les utilisateurs connectés
            MessageWebSocket.sendMessageToUser(userId, wsMessage);
            
            logger.debug("Notification de présence en ligne envoyée pour l'utilisateur: {}", userId);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification de présence", e);
        }
    }
    
    /**
     * Notifie qu'un utilisateur est hors ligne
     */
    public void notifyUserOffline(java.util.UUID userId) {
        try {
            MessageWebSocket.WebSocketMessage wsMessage = new MessageWebSocket.WebSocketMessage();
            wsMessage.setType("user_offline");
            wsMessage.getData().put("userId", userId.toString());
            wsMessage.getData().put("isOnline", false);
            
            // Envoyer à tous les utilisateurs connectés
            MessageWebSocket.sendMessageToUser(userId, wsMessage);
            
            logger.debug("Notification de présence hors ligne envoyée pour l'utilisateur: {}", userId);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification de présence", e);
        }
    }
    
    /**
     * Notifie une erreur à un utilisateur spécifique
     */
    public void notifyError(java.util.UUID userId, String errorMessage) {
        try {
            MessageWebSocket.WebSocketMessage wsMessage = new MessageWebSocket.WebSocketMessage();
            wsMessage.setType("error");
            wsMessage.getData().put("message", errorMessage);
            
            MessageWebSocket.sendMessageToUser(userId, wsMessage);
            
            logger.debug("Notification d'erreur envoyée à l'utilisateur: {}", userId);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification d'erreur", e);
        }
    }
}
