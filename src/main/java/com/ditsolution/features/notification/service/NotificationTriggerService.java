package com.ditsolution.features.notification.service;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.messaging.entity.ConversationEntity;
import com.ditsolution.features.messaging.entity.MessageEntity;
import com.ditsolution.features.notification.dto.SendNotificationRequest;
import com.ditsolution.features.notification.entity.NotificationEntity;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class NotificationTriggerService {

    @Inject
    NotificationService notificationService;

    @Inject
    EntityManager entityManager;

    /**
     * Déclenche une notification pour une nouvelle annonce correspondant aux critères d'un utilisateur
     */
    @Transactional
    public void triggerNewListingMatchNotification(ListingEntity listing) {
        try {
            // Récupérer les utilisateurs qui pourraient être intéressés par cette annonce
            List<UserEntity> interestedUsers = findInterestedUsers(listing);

            for (UserEntity user : interestedUsers) {
                SendNotificationRequest request = new SendNotificationRequest();
                request.setUserId(user.getId());
                request.setType(NotificationEntity.NotificationType.NEW_LISTING_MATCH);
                request.setTitle("Nouvelle annonce correspondant à vos critères");
                request.setBody(String.format("Un nouveau %s à %s pour %s€", 
                    getListingTypeText(listing.getType().toString()),
                    listing.getCity(),
                    listing.getPrice()));

                Map<String, String> data = new HashMap<>();
                data.put("listingId", listing.getId().toString());
                data.put("listingTitle", listing.getTitle());
                data.put("listingPrice", listing.getPrice().toString());
                data.put("listingCity", listing.getCity());
                request.setData(data);
                request.setRelatedEntityType("listing");
                request.setRelatedEntityId(listing.getId().toString());

                notificationService.sendNotificationToUser(request);
            }

            Log.info("Notifications de nouvelle annonce envoyées à " + interestedUsers.size() + " utilisateurs");
        } catch (Exception e) {
            Log.error("Erreur lors de l'envoi des notifications de nouvelle annonce: " + e.getMessage(), e);
        }
    }

    /**
     * Déclenche une notification pour un nouveau message
     */
    @Transactional
    public void triggerNewMessageNotification(MessageEntity message) {
        try {
            ConversationEntity conversation = message.getConversation();
            UserEntity recipient = null;

            // Déterminer le destinataire (celui qui n'est pas l'expéditeur)
            if (conversation.getTenant().getId().equals(message.getSender().getId())) {
                recipient = conversation.getOwner();
            } else {
                recipient = conversation.getTenant();
            }

            SendNotificationRequest request = new SendNotificationRequest();
            request.setUserId(recipient.getId());
            request.setType(NotificationEntity.NotificationType.NEW_MESSAGE);
            request.setTitle("Nouveau message");
            request.setBody(String.format("%s: %s", 
                message.getSender().getFirstName(),
                truncateMessage(message.getContent(), 50)));

            Map<String, String> data = new HashMap<>();
            data.put("conversationId", conversation.getId().toString());
            data.put("messageId", message.getId().toString());
            data.put("senderName", message.getSender().getFirstName());
            request.setData(data);
            request.setRelatedEntityType("conversation");
            request.setRelatedEntityId(conversation.getId().toString());

            notificationService.sendNotificationToUser(request);
            Log.info("Notification de nouveau message envoyée à l'utilisateur: " + recipient.getId());
        } catch (Exception e) {
            Log.error("Erreur lors de l'envoi de la notification de nouveau message: " + e.getMessage(), e);
        }
    }

    /**
     * Déclenche une notification pour un changement de statut d'annonce
     */
    @Transactional
    public void triggerListingStatusChangeNotification(ListingEntity listing, String oldStatus, String newStatus) {
        try {
            // Notifier le propriétaire de l'annonce
            SendNotificationRequest request = new SendNotificationRequest();
            request.setUserId(listing.getOwner().getId());
            request.setType(NotificationEntity.NotificationType.LISTING_STATUS_CHANGE);
            request.setTitle("Statut de votre annonce mis à jour");
            request.setBody(String.format("Votre annonce \"%s\" est maintenant %s", 
                listing.getTitle(),
                getStatusText(newStatus)));

            Map<String, String> data = new HashMap<>();
            data.put("listingId", listing.getId().toString());
            data.put("oldStatus", oldStatus);
            data.put("newStatus", newStatus);
            request.setData(data);
            request.setRelatedEntityType("listing");
            request.setRelatedEntityId(listing.getId().toString());

            notificationService.sendNotificationToUser(request);
            Log.info("Notification de changement de statut envoyée au propriétaire: " + listing.getOwner().getId());
        } catch (Exception e) {
            Log.error("Erreur lors de l'envoi de la notification de changement de statut: " + e.getMessage(), e);
        }
    }

    /**
     * Déclenche une notification pour une mise à jour d'annonce favorite
     */
    @Transactional
    public void triggerFavoriteUpdateNotification(ListingEntity listing) {
        try {
            // Récupérer les utilisateurs qui ont cette annonce en favori
            List<UserEntity> favoriteUsers = findUsersWithFavorite(listing.getId());

            for (UserEntity user : favoriteUsers) {
                SendNotificationRequest request = new SendNotificationRequest();
                request.setUserId(user.getId());
                request.setType(NotificationEntity.NotificationType.FAVORITE_UPDATE);
                request.setTitle("Mise à jour d'une annonce favorite");
                request.setBody(String.format("L'annonce \"%s\" a été mise à jour", listing.getTitle()));

                Map<String, String> data = new HashMap<>();
                data.put("listingId", listing.getId().toString());
                data.put("listingTitle", listing.getTitle());
                request.setData(data);
                request.setRelatedEntityType("listing");
                request.setRelatedEntityId(listing.getId().toString());

                notificationService.sendNotificationToUser(request);
            }

            Log.info("Notifications de mise à jour favorite envoyées à " + favoriteUsers.size() + " utilisateurs");
        } catch (Exception e) {
            Log.error("Erreur lors de l'envoi des notifications de mise à jour favorite: " + e.getMessage(), e);
        }
    }

    /**
     * Déclenche une notification système
     */
    @Transactional
    public void triggerSystemAnnouncement(String title, String body, List<UUID> userIds) {
        try {
            for (UUID userId : userIds) {
                SendNotificationRequest request = new SendNotificationRequest();
                request.setUserId(userId);
                request.setType(NotificationEntity.NotificationType.SYSTEM_ANNOUNCEMENT);
                request.setTitle(title);
                request.setBody(body);
                request.setRelatedEntityType("system");

                notificationService.sendNotificationToUser(request);
            }

            Log.info("Notifications système envoyées à " + userIds.size() + " utilisateurs");
        } catch (Exception e) {
            Log.error("Erreur lors de l'envoi des notifications système: " + e.getMessage(), e);
        }
    }

    // Méthodes privées utilitaires

    private List<UserEntity> findInterestedUsers(ListingEntity listing) {
        // Logique simplifiée : récupérer les utilisateurs actifs
        // Dans une vraie implémentation, vous pourriez filtrer par préférences, localisation, etc.
        TypedQuery<UserEntity> query = entityManager.createQuery(
            "SELECT u FROM UserEntity u WHERE u.status = 'ACTIVE' AND u.id != :ownerId",
            UserEntity.class
        );
        query.setParameter("ownerId", listing.getOwner().getId());
        query.setMaxResults(100); // Limiter pour éviter de spammer tous les utilisateurs
        
        return query.getResultList();
    }

    private List<UserEntity> findUsersWithFavorite(UUID listingId) {
        TypedQuery<UserEntity> query = entityManager.createQuery(
            "SELECT f.user FROM FavoriteEntity f WHERE f.listing.id = :listingId",
            UserEntity.class
        );
        query.setParameter("listingId", listingId);
        
        return query.getResultList();
    }

    private String getListingTypeText(String type) {
        switch (type) {
            case "APARTMENT":
                return "appartement";
            case "HOUSE":
                return "maison";
            case "STUDIO":
                return "studio";
            case "OFFICE":
                return "bureau";
            case "COMMERCIAL":
                return "local commercial";
            default:
                return "bien";
        }
    }

    private String getStatusText(String status) {
        switch (status) {
            case "DRAFT":
                return "en brouillon";
            case "ACTIVE":
                return "active";
            case "SOLD":
                return "vendue";
            case "RENTED":
                return "louée";
            case "INACTIVE":
                return "inactive";
            default:
                return status.toLowerCase();
        }
    }

    private String truncateMessage(String message, int maxLength) {
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }
}

