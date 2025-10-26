package com.ditsolution.features.notification.service;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.notification.entity.UserNotificationPreferencesEntity;
import com.ditsolution.features.notification.dto.NotificationPreferencesDto;
import com.ditsolution.features.notification.dto.UpdateNotificationPreferencesRequest;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class NotificationPreferencesService {

    @Inject
    EntityManager entityManager;

    /**
     * Récupère les préférences de notifications d'un utilisateur
     */
    public NotificationPreferencesDto getUserPreferences(UUID userId) {
        UserNotificationPreferencesEntity preferences = findOrCreatePreferences(userId);
        return mapToDto(preferences);
    }

    /**
     * Met à jour les préférences de notifications d'un utilisateur
     */
    @Transactional
    public NotificationPreferencesDto updateUserPreferences(UUID userId, UpdateNotificationPreferencesRequest request) {
        UserNotificationPreferencesEntity preferences = findOrCreatePreferences(userId);
        
        // Mettre à jour les préférences
        if (request.getPaymentReminders() != null) {
            preferences.setPaymentReminders(request.getPaymentReminders());
        }
        if (request.getOverdueAlerts() != null) {
            preferences.setOverdueAlerts(request.getOverdueAlerts());
        }
        if (request.getPaymentConfirmations() != null) {
            preferences.setPaymentConfirmations(request.getPaymentConfirmations());
        }
        if (request.getNewContracts() != null) {
            preferences.setNewContracts(request.getNewContracts());
        }
        if (request.getNewMessages() != null) {
            preferences.setNewMessages(request.getNewMessages());
        }
        if (request.getListingStatusChanges() != null) {
            preferences.setListingStatusChanges(request.getListingStatusChanges());
        }
        if (request.getFavoriteUpdates() != null) {
            preferences.setFavoriteUpdates(request.getFavoriteUpdates());
        }
        if (request.getSystemUpdates() != null) {
            preferences.setSystemUpdates(request.getSystemUpdates());
        }
        if (request.getMarketingNotifications() != null) {
            preferences.setMarketingNotifications(request.getMarketingNotifications());
        }
        if (request.getReminderDays() != null) {
            preferences.setReminderDays(request.getReminderDays());
        }
        if (request.getReminderTime() != null) {
            preferences.setReminderTime(request.getReminderTime());
        }
        if (request.getOverdueFrequency() != null) {
            preferences.setOverdueFrequency(request.getOverdueFrequency());
        }
        if (request.getPushEnabled() != null) {
            preferences.setPushEnabled(request.getPushEnabled());
        }
        if (request.getEmailEnabled() != null) {
            preferences.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getSmsEnabled() != null) {
            preferences.setSmsEnabled(request.getSmsEnabled());
        }
        if (request.getQuietHoursEnabled() != null) {
            preferences.setQuietHoursEnabled(request.getQuietHoursEnabled());
        }
        if (request.getQuietHoursStart() != null) {
            preferences.setQuietHoursStart(request.getQuietHoursStart());
        }
        if (request.getQuietHoursEnd() != null) {
            preferences.setQuietHoursEnd(request.getQuietHoursEnd());
        }
        if (request.getNotificationFrequency() != null) {
            preferences.setNotificationFrequency(request.getNotificationFrequency());
        }

        entityManager.merge(preferences);
        Log.info("Préférences de notifications mises à jour pour l'utilisateur: " + userId);
        
        return mapToDto(preferences);
    }

    /**
     * Réinitialise les préférences d'un utilisateur aux valeurs par défaut
     */
    @Transactional
    public NotificationPreferencesDto resetToDefaults(UUID userId) {
        UserNotificationPreferencesEntity preferences = findOrCreatePreferences(userId);
        
        // Réinitialiser aux valeurs par défaut
        preferences.setPaymentReminders(true);
        preferences.setOverdueAlerts(true);
        preferences.setPaymentConfirmations(true);
        preferences.setNewContracts(true);
        preferences.setNewMessages(true);
        preferences.setListingStatusChanges(true);
        preferences.setFavoriteUpdates(true);
        preferences.setSystemUpdates(true);
        preferences.setMarketingNotifications(false);
        preferences.setReminderDays(new Integer[]{1, 3, 7});
        preferences.setReminderTime(java.time.LocalTime.of(9, 0));
        preferences.setOverdueFrequency(UserNotificationPreferencesEntity.OverdueFrequency.DAILY);
        preferences.setPushEnabled(true);
        preferences.setEmailEnabled(true);
        preferences.setSmsEnabled(false);
        preferences.setQuietHoursEnabled(false);
        preferences.setQuietHoursStart(java.time.LocalTime.of(22, 0));
        preferences.setQuietHoursEnd(java.time.LocalTime.of(8, 0));
        preferences.setNotificationFrequency(UserNotificationPreferencesEntity.NotificationFrequency.IMMEDIATE);

        entityManager.merge(preferences);
        Log.info("Préférences de notifications réinitialisées pour l'utilisateur: " + userId);
        
        return mapToDto(preferences);
    }

    /**
     * Trouve ou crée les préférences pour un utilisateur
     */
    private UserNotificationPreferencesEntity findOrCreatePreferences(UUID userId) {
        UserNotificationPreferencesEntity preferences = entityManager.createQuery(
            "SELECT p FROM UserNotificationPreferencesEntity p WHERE p.user.id = :userId",
            UserNotificationPreferencesEntity.class
        )
        .setParameter("userId", userId)
        .getResultStream()
        .findFirst()
        .orElse(null);

        if (preferences == null) {
            UserEntity user = entityManager.find(UserEntity.class, userId);
            if (user == null) {
                throw new IllegalArgumentException("Utilisateur non trouvé: " + userId);
            }
            
            preferences = new UserNotificationPreferencesEntity(user);
            entityManager.persist(preferences);
            Log.info("Nouvelles préférences de notifications créées pour l'utilisateur: " + userId);
        }

        return preferences;
    }

    /**
     * Convertit l'entité en DTO
     */
    private NotificationPreferencesDto mapToDto(UserNotificationPreferencesEntity preferences) {
        NotificationPreferencesDto dto = new NotificationPreferencesDto();
        dto.setId(preferences.getId());
        dto.setUserId(preferences.getUser().getId());
        dto.setPaymentReminders(preferences.getPaymentReminders());
        dto.setOverdueAlerts(preferences.getOverdueAlerts());
        dto.setPaymentConfirmations(preferences.getPaymentConfirmations());
        dto.setNewContracts(preferences.getNewContracts());
        dto.setNewMessages(preferences.getNewMessages());
        dto.setListingStatusChanges(preferences.getListingStatusChanges());
        dto.setFavoriteUpdates(preferences.getFavoriteUpdates());
        dto.setSystemUpdates(preferences.getSystemUpdates());
        dto.setMarketingNotifications(preferences.getMarketingNotifications());
        dto.setReminderDays(preferences.getReminderDays());
        dto.setReminderTime(preferences.getReminderTime());
        dto.setOverdueFrequency(preferences.getOverdueFrequency());
        dto.setPushEnabled(preferences.getPushEnabled());
        dto.setEmailEnabled(preferences.getEmailEnabled());
        dto.setSmsEnabled(preferences.getSmsEnabled());
        dto.setQuietHoursEnabled(preferences.getQuietHoursEnabled());
        dto.setQuietHoursStart(preferences.getQuietHoursStart());
        dto.setQuietHoursEnd(preferences.getQuietHoursEnd());
        dto.setNotificationFrequency(preferences.getNotificationFrequency());
        dto.setCreatedAt(preferences.getCreatedAt());
        dto.setUpdatedAt(preferences.getUpdatedAt());
        
        return dto;
    }
}
