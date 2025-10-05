package com.ditsolution.features.messaging.websocket;

import com.ditsolution.features.messaging.service.MessageService;
import com.ditsolution.features.messaging.service.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/messages/{token}")
@ApplicationScoped
public class MessageWebSocket {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageWebSocket.class);
    
    @Inject
    MessageService messageService;
    
    @Inject
    ConversationService conversationService;
    
    @Inject
    ObjectMapper objectMapper;
    
    @Inject
    JWTParser jwtParser;
    
    // Map pour stocker les sessions WebSocket par utilisateur
    private static final Map<UUID, Session> userSessions = new ConcurrentHashMap<>();
    
    // Map pour stocker les sessions par ID de session
    private static final Map<String, UUID> sessionUsers = new ConcurrentHashMap<>();
    
    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        try {
            // Valider le token JWT et récupérer l'utilisateur
            UUID userId = extractUserIdFromToken(token);
            
            if (userId != null) {
                userSessions.put(userId, session);
                sessionUsers.put(session.getId(), userId);
                
                logger.info("WebSocket connecté pour l'utilisateur: {}", userId);
                
                // Envoyer un message de confirmation
                sendMessage(session, createConnectionMessage("connected"));
            } else {
                logger.warn("Token invalide pour la connexion WebSocket");
                session.close();
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'ouverture de la session WebSocket", e);
            try {
                session.close();
            } catch (IOException ioException) {
                logger.error("Erreur lors de la fermeture de la session", ioException);
            }
        }
    }
    
    @OnClose
    public void onClose(Session session) {
        UUID userId = sessionUsers.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
            logger.info("WebSocket fermé pour l'utilisateur: {}", userId);
        }
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        UUID userId = sessionUsers.get(session.getId());
        logger.error("Erreur WebSocket pour l'utilisateur: {}", userId, throwable);
        
        // Nettoyer la session en cas d'erreur
        if (userId != null) {
            userSessions.remove(userId);
            sessionUsers.remove(session.getId());
        }
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            UUID userId = sessionUsers.get(session.getId());
            if (userId == null) {
                logger.warn("Session non authentifiée");
                return;
            }
            
            // Parser le message JSON
            WebSocketMessage wsMessage = objectMapper.readValue(message, WebSocketMessage.class);
            
            switch (wsMessage.getType()) {
                case "ping":
                    sendMessage(session, createConnectionMessage("pong"));
                    break;
                case "typing":
                    handleTypingMessage(wsMessage, userId);
                    break;
                default:
                    logger.warn("Type de message WebSocket non reconnu: {}", wsMessage.getType());
            }
        } catch (Exception e) {
            logger.error("Erreur lors du traitement du message WebSocket", e);
        }
    }
    
    /**
     * Envoie un message à un utilisateur spécifique
     */
    public static void sendMessageToUser(UUID userId, Object message) {
        Session session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        }
    }
    
    /**
     * Envoie un message à tous les utilisateurs d'une conversation
     */
    public static void sendMessageToConversation(Long conversationId, Object message, UUID excludeUserId) {
        // TODO: Implémenter la logique pour envoyer à tous les participants d'une conversation
        // Pour l'instant, on envoie à tous les utilisateurs connectés
        userSessions.forEach((userId, session) -> {
            if (!userId.equals(excludeUserId)) {
                sendMessage(session, message);
            }
        });
    }
    
    /**
     * Envoie un message via une session
     */
    private static void sendMessage(Session session, Object message) {
        try {
            if (session.isOpen()) {
                ObjectMapper mapper = new ObjectMapper();
                String jsonMessage = mapper.writeValueAsString(message);
                session.getBasicRemote().sendText(jsonMessage);
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi du message WebSocket", e);
        }
    }
    
    /**
     * Gère les messages de frappe
     */
    private void handleTypingMessage(WebSocketMessage wsMessage, UUID userId) {
        try {
            Long conversationId = Long.valueOf(wsMessage.getData().get("conversationId").toString());
            Boolean isTyping = Boolean.valueOf(wsMessage.getData().get("isTyping").toString());
            
            // Créer un message de frappe
            WebSocketMessage typingMessage = new WebSocketMessage();
            typingMessage.setType("typing");
            typingMessage.getData().put("conversationId", conversationId);
            typingMessage.getData().put("userId", userId);
            typingMessage.getData().put("isTyping", isTyping);
            
            // Envoyer à tous les autres participants de la conversation
            sendMessageToConversation(conversationId, typingMessage, userId);
        } catch (Exception e) {
            logger.error("Erreur lors du traitement du message de frappe", e);
        }
    }
    
    /**
     * Crée un message de connexion
     */
    private WebSocketMessage createConnectionMessage(String status) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("connection");
        message.getData().put("status", status);
        return message;
    }
    
    /**
     * Extrait l'ID utilisateur du token JWT
     */
    private UUID extractUserIdFromToken(String token) {
        try {
            // Parser le token JWT
            JsonWebToken jwt = jwtParser.parse(token);
            
            // Extraire l'ID utilisateur du token
            String userIdString = jwt.getSubject();
            if (userIdString != null) {
                return UUID.fromString(userIdString);
            }
            
            return null;
        } catch (ParseException | IllegalArgumentException e) {
            logger.error("Erreur lors de l'extraction de l'ID utilisateur du token", e);
            return null;
        }
    }
    
    /**
     * Classe pour les messages WebSocket
     */
    public static class WebSocketMessage {
        private String type;
        private Map<String, Object> data = new ConcurrentHashMap<>();
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Map<String, Object> getData() {
            return data;
        }
        
        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }
}
