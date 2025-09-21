package com.ditsolution.features.storage.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ScheduledTasksService {

    private static final Logger LOG = Logger.getLogger(ScheduledTasksService.class);

    @Inject
    ThumbnailService thumbnailService;

    @Inject
    S3CleanupService s3CleanupService;

    @ConfigProperty(name = "app.scheduler.thumbnail-generation.enabled", defaultValue = "true")
    boolean thumbnailGenerationEnabled;

    @ConfigProperty(name = "app.scheduler.s3-cleanup.enabled", defaultValue = "true")
    boolean s3CleanupEnabled;

    /**
     * Génère des miniatures pour les nouvelles images
     * Exécuté toutes les heures
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void generateThumbnailsScheduled() {
        if (!thumbnailGenerationEnabled) {
            LOG.debug("Génération de miniatures désactivée par configuration");
            return;
        }

        try {
            LOG.info("Début de la tâche planifiée de génération de miniatures");
            int generatedCount = thumbnailService.generateThumbnailsForAllImages();
            LOG.info("Tâche planifiée de génération de miniatures terminée - " + generatedCount + " miniatures générées");
        } catch (Exception e) {
            LOG.error("Erreur lors de la tâche planifiée de génération de miniatures", e);
        }
    }

    /**
     * Nettoie les images non utilisées depuis plus de 30 jours
     * Exécuté tous les jours à 2h du matin
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupUnusedImagesScheduled() {
        if (!s3CleanupEnabled) {
            LOG.debug("Nettoyage S3 désactivé par configuration");
            return;
        }

        try {
            LOG.info("Début de la tâche planifiée de nettoyage des images non utilisées");
            S3CleanupService.CleanupResult result = s3CleanupService.cleanupUnusedImages();
            LOG.info("Tâche planifiée de nettoyage terminée - " + result.toString());
        } catch (Exception e) {
            LOG.error("Erreur lors de la tâche planifiée de nettoyage des images", e);
        }
    }

    /**
     * Nettoyage complet des images non utilisées
     * Exécuté tous les dimanches à 3h du matin
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void fullCleanupScheduled() {
        if (!s3CleanupEnabled) {
            LOG.debug("Nettoyage S3 complet désactivé par configuration");
            return;
        }

        try {
            LOG.info("Début de la tâche planifiée de nettoyage complet");
            S3CleanupService.CleanupResult result = s3CleanupService.cleanupAllUnusedImages();
            LOG.info("Tâche planifiée de nettoyage complet terminée - " + result.toString());
        } catch (Exception e) {
            LOG.error("Erreur lors de la tâche planifiée de nettoyage complet", e);
        }
    }

    /**
     * Tâche de maintenance hebdomadaire
     * Exécuté tous les lundis à 1h du matin
     */
    @Scheduled(cron = "0 0 1 ? * MON")
    public void weeklyMaintenanceScheduled() {
        try {
            LOG.info("Début de la maintenance hebdomadaire");
            
            // Générer les miniatures manquantes
            if (thumbnailGenerationEnabled) {
                int generatedCount = thumbnailService.generateThumbnailsForAllImages();
                LOG.info("Maintenance: " + generatedCount + " miniatures générées");
            }
            
            // Nettoyage des images non utilisées
            if (s3CleanupEnabled) {
                S3CleanupService.CleanupResult result = s3CleanupService.cleanupUnusedImages();
                LOG.info("Maintenance: " + result.toString());
            }
            
            LOG.info("Maintenance hebdomadaire terminée");
        } catch (Exception e) {
            LOG.error("Erreur lors de la maintenance hebdomadaire", e);
        }
    }
}
