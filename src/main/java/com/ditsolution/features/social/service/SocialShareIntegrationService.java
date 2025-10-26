package com.ditsolution.features.social.service;

import com.ditsolution.features.social.dto.CreateSocialShareRequest;
import com.ditsolution.features.social.service.SocialShareService;
import com.ditsolution.features.auth.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class SocialShareIntegrationService {

    private static final Logger LOG = Logger.getLogger(SocialShareIntegrationService.class);

    @Inject
    SocialShareService socialShareService;

    /**
     * Tracker un partage depuis le frontend (AnalyticsService.trackConversion)
     */
    @Transactional
    public void trackShareFromFrontend(String propertyId, String userId, String platform, 
                                     String shareType, Map<String, Object> metadata) {
        try {
            // Convertir les paramètres
            UUID listingId = null;
            if (!"social_share".equals(propertyId)) {
                try {
                    listingId = UUID.fromString(propertyId);
                } catch (IllegalArgumentException e) {
                    LOG.warn("ID de propriété invalide pour le partage: " + propertyId);
                }
            }

            UUID userUuid = null;
            if (!"current_user".equals(userId)) {
                try {
                    userUuid = UUID.fromString(userId);
                } catch (IllegalArgumentException e) {
                    LOG.warn("ID utilisateur invalide pour le partage: " + userId);
                }
            }

            // Déterminer le type de partage
            String shareTypeValue = "property";
            if (metadata != null && metadata.containsKey("shareType")) {
                shareTypeValue = metadata.get("shareType").toString();
            }

            // Créer la requête de partage
            CreateSocialShareRequest request = new CreateSocialShareRequest();
            request.listingId = listingId;
            request.platform = platform;
            request.shareType = shareTypeValue;
            request.metadata = metadata;

            // Récupérer l'utilisateur (si disponible)
            UserEntity user = null;
            if (userUuid != null) {
                user = UserEntity.findById(userUuid);
            }

            // Enregistrer le partage
            if (user != null) {
                socialShareService.recordShare(request, user);
                LOG.info("Partage enregistré: " + platform + " par " + userUuid);
            } else {
                LOG.warn("Impossible d'enregistrer le partage - utilisateur non trouvé: " + userId);
            }

        } catch (Exception e) {
            LOG.error("Erreur lors du tracking du partage depuis le frontend", e);
        }
    }

    /**
     * Synchroniser les données de partage avec le frontend
     */
    public Map<String, Object> getShareStatsForFrontend(UUID listingId) {
        try {
            var stats = socialShareService.getShareStats(listingId);
            
            Map<String, Object> frontendStats = new HashMap<>();
            frontendStats.put("totalShares", stats.totalShares);
            frontendStats.put("sharesByPlatform", stats.sharesByPlatform);
            frontendStats.put("sharesByType", stats.sharesByType);
            frontendStats.put("uniqueUsers", stats.uniqueUsers);
            frontendStats.put("uniqueListings", stats.uniqueListings);
            frontendStats.put("recentShares", stats.recentShares);
            
            return frontendStats;
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des stats de partage", e);
            return new HashMap<>();
        }
    }
}
