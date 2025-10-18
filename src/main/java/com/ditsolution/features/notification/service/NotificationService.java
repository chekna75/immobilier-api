package com.ditsolution.features.notification.service;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.notification.config.FirebaseConfig;
import com.ditsolution.features.notification.dto.NotificationDto;
import com.ditsolution.features.notification.dto.SendNotificationRequest;
import com.ditsolution.features.notification.entity.DeviceTokenEntity;
import com.ditsolution.features.notification.entity.NotificationEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationService {

    @Inject
    EntityManager entityManager;

    @Inject
    FirebaseMessaging firebaseMessaging;

    @Inject
    FirebaseConfig firebaseConfig;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "app.notification.batch-size", defaultValue = "500")
    int batchSize;

    /**
     * Enregistre un device token pour un utilisateur
     */
    @Transactional
    public void registerDeviceToken(UUID userId, String token, DeviceTokenEntity.Platform platform, 
                                   String appVersion, String deviceModel) {
        // Désactiver les anciens tokens pour cet utilisateur et cette plateforme
        entityManager.createQuery(
            "UPDATE DeviceTokenEntity dt SET dt.isActive = false WHERE dt.user.id = :userId AND dt.platform = :platform"
        )
        .setParameter("userId", userId)
        .setParameter("platform", platform)
        .executeUpdate();

        // Créer le nouveau token
        UserEntity user = entityManager.find(UserEntity.class, userId);
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur non trouvé: " + userId);
        }

        DeviceTokenEntity deviceToken = new DeviceTokenEntity(user, token, platform);
        deviceToken.setAppVersion(appVersion);
        deviceToken.setDeviceModel(deviceModel);
        deviceToken.setLastUsedAt(LocalDateTime.now());

        entityManager.persist(deviceToken);
        Log.info("Device token enregistré pour l'utilisateur: " + userId);
    }

    /**
     * Envoie une notification à un utilisateur spécifique
     */
    @Transactional
    public void sendNotificationToUser(SendNotificationRequest request) {
        if (!firebaseConfig.isFirebaseConfigured()) {
            Log.warn("Firebase non configuré, notification non envoyée");
            return;
        }

        UserEntity user = entityManager.find(UserEntity.class, request.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur non trouvé: " + request.getUserId());
        }

        // Créer l'entité notification
        NotificationEntity notification = new NotificationEntity(
            user, 
            request.getType(), 
            request.getTitle(), 
            request.getBody()
        );

        if (request.getData() != null && !request.getData().isEmpty()) {
            try {
                // Convertir la Map en JSON
                String dataJson = objectMapper.writeValueAsString(request.getData());
                notification.setData(dataJson);
            } catch (JsonProcessingException e) {
                Log.error("Erreur lors de la sérialisation des données: " + e.getMessage());
                // En cas d'erreur, stocker comme objet simple
                notification.setData("{\"error\": \"Erreur de sérialisation des données\"}");
            }
        }

        notification.setRelatedEntityType(request.getRelatedEntityType());
        notification.setRelatedEntityId(request.getRelatedEntityId());

        entityManager.persist(notification);

        // Récupérer les tokens actifs de l'utilisateur
        List<DeviceTokenEntity> deviceTokens = getActiveDeviceTokens(request.getUserId());

        if (deviceTokens.isEmpty()) {
            Log.info("Aucun device token actif pour l'utilisateur: " + request.getUserId());
            return;
        }

        // Envoyer la notification
        sendFirebaseNotification(notification, deviceTokens);
    }

    /**
     * Envoie une notification à plusieurs utilisateurs
     */
    @Transactional
    public void sendNotificationToUsers(List<UUID> userIds, SendNotificationRequest request) {
        if (!firebaseConfig.isFirebaseConfigured()) {
            Log.warn("Firebase non configuré, notifications non envoyées");
            return;
        }

        for (UUID userId : userIds) {
            SendNotificationRequest userRequest = new SendNotificationRequest();
            userRequest.setUserId(userId);
            userRequest.setType(request.getType());
            userRequest.setTitle(request.getTitle());
            userRequest.setBody(request.getBody());
            userRequest.setData(request.getData());
            userRequest.setRelatedEntityType(request.getRelatedEntityType());
            userRequest.setRelatedEntityId(request.getRelatedEntityId());

            sendNotificationToUser(userRequest);
        }
    }

    /**
     * Récupère les notifications d'un utilisateur
     */
    public List<NotificationDto> getUserNotifications(UUID userId, int page, int size) {
        TypedQuery<NotificationEntity> query = entityManager.createQuery(
            "SELECT n FROM NotificationEntity n WHERE n.user.id = :userId ORDER BY n.createdAt DESC",
            NotificationEntity.class
        )
        .setParameter("userId", userId)
        .setFirstResult(page * size)
        .setMaxResults(size);

        return query.getResultList().stream()
            .map(NotificationDto::new)
            .collect(Collectors.toList());
    }

    /**
     * Marque une notification comme lue
     */
    @Transactional
    public void markNotificationAsRead(UUID notificationId, UUID userId) {
        NotificationEntity notification = entityManager.createQuery(
            "SELECT n FROM NotificationEntity n WHERE n.id = :id AND n.user.id = :userId",
            NotificationEntity.class
        )
        .setParameter("id", notificationId)
        .setParameter("userId", userId)
        .getSingleResult();

        if (notification != null) {
            notification.markAsRead();
            entityManager.merge(notification);
        }
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues
     */
    @Transactional
    public void markAllNotificationsAsRead(UUID userId) {
        entityManager.createQuery(
            "UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :now WHERE n.user.id = :userId AND n.isRead = false"
        )
        .setParameter("userId", userId)
        .setParameter("now", LocalDateTime.now())
        .executeUpdate();
    }

    /**
     * Compte les notifications non lues d'un utilisateur
     */
    public long getUnreadNotificationCount(UUID userId) {
        return entityManager.createQuery(
            "SELECT COUNT(n) FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false",
            Long.class
        )
        .setParameter("userId", userId)
        .getSingleResult();
    }

    /**
     * Supprime les anciennes notifications (plus de 30 jours)
     */
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        int deletedCount = entityManager.createQuery(
            "DELETE FROM NotificationEntity n WHERE n.createdAt < :cutoffDate"
        )
        .setParameter("cutoffDate", cutoffDate)
        .executeUpdate();

        Log.info("Nettoyage des notifications: " + deletedCount + " notifications supprimées");
    }

    /**
     * Désactive les tokens inactifs (plus de 7 jours sans utilisation)
     */
    @Transactional
    public void deactivateInactiveTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        
        int deactivatedCount = entityManager.createQuery(
            "UPDATE DeviceTokenEntity dt SET dt.isActive = false WHERE dt.lastUsedAt < :cutoffDate AND dt.isActive = true"
        )
        .setParameter("cutoffDate", cutoffDate)
        .executeUpdate();

        Log.info("Désactivation des tokens: " + deactivatedCount + " tokens désactivés");
    }

    // Méthodes privées

    private List<DeviceTokenEntity> getActiveDeviceTokens(UUID userId) {
        return entityManager.createQuery(
            "SELECT dt FROM DeviceTokenEntity dt WHERE dt.user.id = :userId AND dt.isActive = true",
            DeviceTokenEntity.class
        )
        .setParameter("userId", userId)
        .getResultList();
    }

    private void sendFirebaseNotification(NotificationEntity notification, List<DeviceTokenEntity> deviceTokens) {
        try {
            // Vérifier si Firebase est configuré
            if (!firebaseConfig.isFirebaseConfigured() || firebaseMessaging == null) {
                Log.warn("Firebase non configuré, simulation d'envoi de notification");
                
                // Marquer comme envoyé même sans Firebase pour les tests
                notification.markAsSent();
                entityManager.merge(notification);
                
                Log.info("Notification simulée envoyée à " + deviceTokens.size() + " appareil(s)");
                return;
            }

            // Préparer les données
            Map<String, String> data = new HashMap<>();
            data.put("notificationId", notification.getId().toString());
            data.put("type", notification.getType().toString());
            
            if (notification.getRelatedEntityType() != null) {
                data.put("relatedEntityType", notification.getRelatedEntityType());
            }
            if (notification.getRelatedEntityId() != null) {
                data.put("relatedEntityId", notification.getRelatedEntityId());
            }
            if (notification.getData() != null) {
                data.put("customData", notification.getData());
            }

            // Créer le message Firebase
            MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getBody())
                    .build())
                .putAllData(data)
                .addAllTokens(deviceTokens.stream()
                    .map(DeviceTokenEntity::getToken)
                    .collect(Collectors.toList()))
                .build();

            // Envoyer le message
            BatchResponse response = firebaseMessaging.sendMulticast(message);
            
            // Marquer comme envoyé
            notification.markAsSent();
            entityManager.merge(notification);

            Log.info("Notification envoyée: " + response.getSuccessCount() + " succès, " + 
                    response.getFailureCount() + " échecs");

            // Gérer les tokens invalides
            handleInvalidTokens(response, deviceTokens);

        } catch (FirebaseMessagingException e) {
            Log.error("Erreur lors de l'envoi de la notification: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.error("Erreur inattendue lors de l'envoi de la notification: " + e.getMessage(), e);
        }
    }

    private void handleInvalidTokens(BatchResponse response, List<DeviceTokenEntity> deviceTokens) {
        List<String> invalidTokens = new ArrayList<>();
        
        for (int i = 0; i < response.getResponses().size(); i++) {
            SendResponse sendResponse = response.getResponses().get(i);
            if (!sendResponse.isSuccessful()) {
                MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();
                if (errorCode == MessagingErrorCode.INVALID_ARGUMENT ||
                    errorCode == MessagingErrorCode.UNREGISTERED) {
                    invalidTokens.add(deviceTokens.get(i).getToken());
                }
            }
        }

        if (!invalidTokens.isEmpty()) {
            // Désactiver les tokens invalides
            entityManager.createQuery(
                "UPDATE DeviceTokenEntity dt SET dt.isActive = false WHERE dt.token IN :tokens"
            )
            .setParameter("tokens", invalidTokens)
            .executeUpdate();

            Log.info("Tokens invalides désactivés: " + invalidTokens.size());
        }
    }
}

