package com.ditsolution.features.notification.dto;

import com.ditsolution.features.notification.entity.NotificationEntity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NotificationDto {
    private UUID id;
    private NotificationEntity.NotificationType type;
    private String title;
    private String body;
    private String data;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime clickedAt;
    private LocalDateTime createdAt;
    private String relatedEntityType;
    private String relatedEntityId;

    public NotificationDto() {}

    public NotificationDto(NotificationEntity entity) {
        this.id = entity.getId();
        this.type = entity.getType();
        this.title = entity.getTitle();
        this.body = entity.getBody();
        this.data = entity.getData();
        this.isRead = entity.getIsRead();
        this.readAt = entity.getReadAt();
        this.sentAt = entity.getSentAt();
        this.deliveredAt = entity.getDeliveredAt();
        this.clickedAt = entity.getClickedAt();
        this.createdAt = entity.getCreatedAt();
        this.relatedEntityType = entity.getRelatedEntityType();
        this.relatedEntityId = entity.getRelatedEntityId();
    }
}

