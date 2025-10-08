package com.ditsolution.features.messaging.mapper;

import com.ditsolution.features.auth.dto.UserDto;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.dto.ListingDto;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.mapper.ListingMapper;
import com.ditsolution.features.messaging.dto.ConversationDto;
import com.ditsolution.features.messaging.entity.ConversationEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConversationMapper {
    
    @Inject
    ListingMapper listingMapper;
    
    /**
     * Convertit une entité Conversation en DTO
     */
    public ConversationDto toDto(ConversationEntity conversation, UserEntity currentUser) {
        ConversationDto dto = new ConversationDto();
        
        dto.setId(conversation.getId());
        dto.setProperty(listingMapper.toDto(conversation.getProperty()));
        dto.setOtherUser(getOtherUserDto(conversation, currentUser));
        dto.setLastMessage(conversation.getLastMessage());
        dto.setLastMessageTime(conversation.getLastMessageTime());
        dto.setUnreadCount(conversation.getUnreadCountForUser(currentUser));
        dto.setIsOnline(false); // TODO: Implémenter la logique de statut en ligne
        dto.setIsArchived(conversation.getIsArchived());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * Récupère les informations de l'autre utilisateur dans la conversation
     */
    private UserDto getOtherUserDto(ConversationEntity conversation, UserEntity currentUser) {
        UserEntity otherUser = conversation.getOtherUser(currentUser);
        if (otherUser == null) {
            return null;
        }
        
        return new UserDto(
            otherUser.id,
            otherUser.role,
            otherUser.status,
            otherUser.email,
            null, // Ne pas exposer le téléphone
            false, // Ne pas exposer le statut de vérification
            otherUser.emailVerified,
            otherUser.firstName,
            otherUser.lastName,
            null, // Ne pas exposer l'avatar
            otherUser.createdAt,
            otherUser.updatedAt
        );
    }
    
    /**
     * Convertit une entité Conversation en DTO pour les administrateurs
     */
    public ConversationDto toDtoForAdmin(ConversationEntity conversation) {
        ConversationDto dto = new ConversationDto();
        
        dto.setId(conversation.getId());
        dto.setProperty(listingMapper.toDto(conversation.getProperty()));
        dto.setOtherUser(getTenantDto(conversation));
        dto.setLastMessage(conversation.getLastMessage());
        dto.setLastMessageTime(conversation.getLastMessageTime());
        dto.setUnreadCount(conversation.getTenantUnreadCount() + conversation.getOwnerUnreadCount());
        dto.setIsOnline(false); // TODO: Implémenter la logique de statut en ligne
        dto.setIsArchived(conversation.getIsArchived());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * Récupère les informations du locataire pour l'administration
     */
    private UserDto getTenantDto(ConversationEntity conversation) {
        UserEntity tenant = conversation.getTenant();
        if (tenant == null) {
            return null;
        }
        
        return new UserDto(
            tenant.id,
            tenant.role,
            tenant.status,
            tenant.email,
            null, // Ne pas exposer le téléphone
            false, // Ne pas exposer le statut de vérification
            tenant.emailVerified,
            tenant.firstName,
            tenant.lastName,
            null, // Ne pas exposer l'avatar
            tenant.createdAt,
            tenant.updatedAt
        );
    }
}
