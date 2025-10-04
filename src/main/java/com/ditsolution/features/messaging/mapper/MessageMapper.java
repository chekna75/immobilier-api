package com.ditsolution.features.messaging.mapper;

import com.ditsolution.features.auth.dto.UserDto;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.messaging.dto.MessageDto;
import com.ditsolution.features.messaging.entity.MessageEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MessageMapper {
    
    /**
     * Convertit une entité Message en DTO
     */
    public MessageDto toDto(MessageEntity message) {
        MessageDto dto = new MessageDto();
        
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSender(getSenderDto(message.getSender()));
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType().name());
        dto.setIsRead(message.getIsRead());
        dto.setReadAt(message.getReadAt());
        dto.setCreatedAt(message.getCreatedAt());
        
        return dto;
    }
    
    /**
     * Récupère les informations de l'expéditeur
     */
    private UserDto getSenderDto(UserEntity sender) {
        return new UserDto(
            sender.id,
            sender.role,
            sender.status,
            sender.email,
            null, // Ne pas exposer le téléphone
            false, // Ne pas exposer le statut de vérification
            sender.emailVerified,
            sender.firstName,
            sender.lastName,
            null, // Ne pas exposer l'avatar
            sender.createdAt,
            sender.updatedAt
        );
    }
}
