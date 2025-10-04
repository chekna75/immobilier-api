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
        // Récupérer la propriété
        ListingEntity property = listingRepository.findById(request.getPropertyId());
        if (property == null) {
            throw new RuntimeException("Propriété non trouvée");
        }
        
        // Déterminer le rôle de l'utilisateur
        UserEntity tenant = currentUser;
        UserEntity owner = property.getOwner();
        
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
    public ConversationDto getConversationByProperty(Long propertyId, UserEntity user) {
        ListingEntity property = listingRepository.findById(propertyId);
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
}
