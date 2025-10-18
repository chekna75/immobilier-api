package com.ditsolution.features.notification.entity;

import com.ditsolution.features.auth.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@EqualsAndHashCode(callSuper = false)
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data; // JSON string pour les données additionnelles

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Référence vers l'entité liée (optionnel)
    @Column(name = "related_entity_type")
    private String relatedEntityType; // "listing", "message", "conversation", etc.

    @Column(name = "related_entity_id")
    private String relatedEntityId;

    public enum NotificationType {
        NEW_LISTING_MATCH,      // Nouvelle annonce correspondant aux critères
        NEW_MESSAGE,            // Nouveau message reçu
        LISTING_STATUS_CHANGE,  // Changement de statut d'annonce
        PAYMENT_REMINDER,       // Rappel de paiement
        SYSTEM_ANNOUNCEMENT,    // Annonce système
        FAVORITE_UPDATE,        // Mise à jour d'une annonce favorite
        CONVERSATION_UPDATE     // Mise à jour de conversation
    }

    // Constructeurs
    public NotificationEntity() {}

    public NotificationEntity(UserEntity user, NotificationType type, String title, String body) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.body = body;
    }

    public NotificationEntity(UserEntity user, NotificationType type, String title, String body, String data) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.body = body;
        this.data = data;
    }

    // Méthodes utilitaires
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public void markAsSent() {
        this.sentAt = LocalDateTime.now();
    }

    public void markAsDelivered() {
        this.deliveredAt = LocalDateTime.now();
    }

    public void markAsClicked() {
        this.clickedAt = LocalDateTime.now();
    }
}

