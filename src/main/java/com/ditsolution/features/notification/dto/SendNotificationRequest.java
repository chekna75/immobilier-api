package com.ditsolution.features.notification.dto;

import com.ditsolution.features.notification.entity.NotificationEntity;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

@Data
public class SendNotificationRequest {
    
    @NotNull(message = "L'ID utilisateur est requis")
    private UUID userId;
    
    @NotNull(message = "Le type de notification est requis")
    private NotificationEntity.NotificationType type;
    
    @NotBlank(message = "Le titre est requis")
    private String title;
    
    @NotBlank(message = "Le contenu est requis")
    private String body;
    
    private Map<String, String> data;
    private String relatedEntityType;
    private String relatedEntityId;
}

