package com.ditsolution.features.notification.service;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NotificationCleanupService {

    @Inject
    NotificationService notificationService;

    /**
     * Nettoie les anciennes notifications et tokens inactifs
     * Exécuté tous les jours à 2h du matin
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupNotifications() {
        try {
            Log.info("Début du nettoyage des notifications");
            
            // Nettoyer les anciennes notifications (plus de 30 jours)
            notificationService.cleanupOldNotifications();
            
            // Désactiver les tokens inactifs (plus de 7 jours sans utilisation)
            notificationService.deactivateInactiveTokens();
            
            Log.info("Nettoyage des notifications terminé");
        } catch (Exception e) {
            Log.error("Erreur lors du nettoyage des notifications: " + e.getMessage(), e);
        }
    }
}

