package com.ditsolution.features.messaging.service;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.repository.ListingRepository;
import com.ditsolution.features.messaging.dto.ConversationDto;
import com.ditsolution.features.messaging.dto.CreateConversationRequest;
import com.ditsolution.features.messaging.entity.ConversationEntity;
import com.ditsolution.features.messaging.entity.MessageEntity;
import com.ditsolution.features.messaging.repository.ConversationRepository;
import com.ditsolution.features.messaging.repository.MessageRepository;
import com.ditsolution.features.messaging.mapper.ConversationMapper;
import com.ditsolution.shared.dto.PagedResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConversationService {
    
    @Inject
    ConversationRepository conversationRepository;
    
    @Inject
    MessageRepository messageRepository;
    
    @Inject
    ListingRepository listingRepository;
    
    @Inject
    ConversationMapper conversationMapper;
    
    /**
     * Crée une nouvelle conversation ou retourne une conversation existante
     */
    @Transactional
    public ConversationDto createOrGetConversation(CreateConversationRequest request, UserEntity currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        
        // Récupérer la propriété
        ListingEntity property = listingRepository.findById(UUID.fromString(request.getPropertyId()));
        if (property == null) {
            throw new RuntimeException("Propriété non trouvée");
        }
        
        // Déterminer le rôle de l'utilisateur
        UserEntity owner = property.getOwner();
        UserEntity tenant;
        
        if (currentUser.equals(owner)) {
            // L'utilisateur actuel est le propriétaire, il ne peut pas créer de conversation
            throw new RuntimeException("Un propriétaire ne peut pas créer de conversation pour sa propre propriété");
        } else {
            // L'utilisateur actuel est le locataire
            tenant = currentUser;
        }
        
        // Vérifier si une conversation existe déjà
        Optional<ConversationEntity> existingConversation = conversationRepository
            .findByPropertyAndUsers(property, tenant, owner);
        
        if (existingConversation.isPresent()) {
            // Retourner la conversation existante
            return conversationMapper.toDto(existingConversation.get(), currentUser);
        }
        
        // Créer une nouvelle conversation
        ConversationEntity conversation = new ConversationEntity(property, tenant, owner);
        conversationRepository.persist(conversation);
        
        // Si un message initial est fourni, l'ajouter
        if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
            MessageEntity initialMessage = new MessageEntity(
                conversation, 
                currentUser, 
                request.getInitialMessage()
            );
            messageRepository.persist(initialMessage);
            
            // Mettre à jour la conversation avec le dernier message
            conversation.setLastMessage(request.getInitialMessage());
            conversation.setLastMessageTime(LocalDateTime.now());
            conversation.incrementUnreadCount(owner);
            conversationRepository.persist(conversation);
        }
        
        return conversationMapper.toDto(conversation, currentUser);
    }
    
    /**
     * Récupère toutes les conversations d'un utilisateur
     */
    public PagedResponse<ConversationDto> getUserConversations(UserEntity user, int page, int size) {
        List<ConversationEntity> conversations = conversationRepository
            .findActiveByUser(user);
        
        // Pagination manuelle
        int start = page * size;
        int end = Math.min(start + size, conversations.size());
        List<ConversationEntity> pagedConversations = conversations.subList(start, end);
        
        List<ConversationDto> conversationDtos = pagedConversations
            .stream()
            .map(conv -> conversationMapper.toDto(conv, user))
            .collect(Collectors.toList());
        
        // int totalPages = (int) Math.ceil((double) conversations.size() / size);
        
        return new PagedResponse<ConversationDto>(
            conversationDtos,
            (long) conversations.size(),
            page,
            size
        );
    }
    
    /**
     * Récupère une conversation spécifique
     */
    public ConversationDto getConversation(Long conversationId, UserEntity user) {
        ConversationEntity conversation = conversationRepository
            .findByIdAndUser(conversationId, user)
            .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
        
        return conversationMapper.toDto(conversation, user);
    }
    
    /**
     * Récupère une conversation par propriété
     */
    public ConversationDto getConversationByProperty(String propertyId, UserEntity user) {
        ListingEntity property = listingRepository.findById(UUID.fromString(propertyId));
        if (property == null) {
            throw new RuntimeException("Propriété non trouvée");
        }
        
        ConversationEntity conversation = conversationRepository
            .findByPropertyAndUser(property, user)
            .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
        
        return conversationMapper.toDto(conversation, user);
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
    }
    
    /**
     * Archive une conversation
     */
    @Transactional
    public void archiveConversation(Long conversationId, UserEntity user) {
        ConversationEntity conversation = conversationRepository
            .findByIdAndUser(conversationId, user)
            .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
        
        conversation.setIsArchived(true);
        conversationRepository.persist(conversation);
    }
    
    /**
     * Supprime une conversation
     */
    @Transactional
    public void deleteConversation(Long conversationId, UserEntity user) {
        ConversationEntity conversation = conversationRepository
            .findByIdAndUser(conversationId, user)
            .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
        
        // Supprimer tous les messages
        messageRepository.deleteByConversation(conversation);
        
        // Marquer la conversation comme inactive
        conversation.setIsActive(false);
        conversationRepository.persist(conversation);
    }
    
    /**
     * Récupère le nombre de messages non lus pour un utilisateur
     */
    public Integer getUnreadCount(UserEntity user) {
        return conversationRepository.countUnreadMessagesForUser(user);
    }
    
    /**
     * Recherche des conversations
     */
    public List<ConversationDto> searchConversations(UserEntity user, String searchTerm) {
        List<ConversationEntity> conversations = conversationRepository
            .searchByUserAndTerm(user, searchTerm);
        
        return conversations.stream()
            .map(conv -> conversationMapper.toDto(conv, user))
            .collect(Collectors.toList());
    }
    
    // ===== MÉTHODES D'ADMINISTRATION =====
    
    /**
     * Récupère toutes les conversations pour les administrateurs
     */
    public PagedResponse<ConversationDto> getAllConversationsForAdmin(int page, int size, String searchTerm, Boolean archived) {
        List<ConversationEntity> conversations;
        
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            conversations = conversationRepository.searchAllConversations(searchTerm);
        } else if (archived != null) {
            conversations = conversationRepository.findByArchivedStatus(archived);
        } else {
            conversations = conversationRepository.findAll().list();
        }
        
        // Pagination manuelle
        int start = page * size;
        int end = Math.min(start + size, conversations.size());
        List<ConversationEntity> pagedConversations = conversations.subList(start, end);
        
        List<ConversationDto> conversationDtos = pagedConversations
            .stream()
            .map(conv -> conversationMapper.toDtoForAdmin(conv))
            .collect(Collectors.toList());
        
        return new PagedResponse<ConversationDto>(
            conversationDtos,
            (long) conversations.size(),
            page,
            size
        );
    }
    
    /**
     * Récupère une conversation pour les administrateurs
     */
    public ConversationDto getConversationForAdmin(Long conversationId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation non trouvée");
        }
        
        return conversationMapper.toDtoForAdmin(conversation);
    }
    
    /**
     * Archive une conversation pour les administrateurs
     */
    @Transactional
    public void archiveConversationForAdmin(Long conversationId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation non trouvée");
        }
        
        conversation.setIsArchived(true);
        conversationRepository.persist(conversation);
    }
    
    /**
     * Désarchive une conversation pour les administrateurs
     */
    @Transactional
    public void unarchiveConversationForAdmin(Long conversationId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation non trouvée");
        }
        
        conversation.setIsArchived(false);
        conversationRepository.persist(conversation);
    }
    
    /**
     * Désactive une conversation pour les administrateurs
     */
    @Transactional
    public void deactivateConversationForAdmin(Long conversationId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation non trouvée");
        }
        
        conversation.setIsActive(false);
        conversationRepository.persist(conversation);
    }
    
    /**
     * Active une conversation pour les administrateurs
     */
    @Transactional
    public void activateConversationForAdmin(Long conversationId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation non trouvée");
        }
        
        conversation.setIsActive(true);
        conversationRepository.persist(conversation);
    }
    
    /**
     * Récupère les statistiques des conversations pour les administrateurs
     */
    public java.util.Map<String, Object> getConversationStatsForAdmin() {
        long totalConversations = conversationRepository.count();
        long activeConversations = conversationRepository.count("isActive = true");
        long archivedConversations = conversationRepository.count("isArchived = true");
        long inactiveConversations = conversationRepository.count("isActive = false");
        
        return java.util.Map.of(
            "totalConversations", totalConversations,
            "activeConversations", activeConversations,
            "archivedConversations", archivedConversations,
            "inactiveConversations", inactiveConversations
        );
    }
}
