package com.ditsolution.features.social.service;

import com.ditsolution.features.social.dto.CreateReviewRequest;
import com.ditsolution.features.social.dto.ReviewDto;
import com.ditsolution.features.social.dto.ReviewStatsDto;
import com.ditsolution.features.auth.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ReviewIntegrationService {

    private static final Logger LOG = Logger.getLogger(ReviewIntegrationService.class);

    @Inject
    ReviewService reviewService;

    /**
     * Créer un avis depuis le frontend (compatible avec ReviewsService.js)
     */
    @Transactional
    public ReviewDto createReviewFromFrontend(Map<String, Object> reviewData, UserEntity user) {
        try {
            // Mapper les données du frontend vers notre format backend
            CreateReviewRequest request = new CreateReviewRequest();
            
            // ID de la cible
            if (reviewData.containsKey("targetId")) {
                request.targetId = UUID.fromString(reviewData.get("targetId").toString());
            }
            
            // Type de cible
            if (reviewData.containsKey("category")) {
                request.targetType = reviewData.get("category").toString();
            } else {
                request.targetType = "property"; // Par défaut
            }
            
            // Note globale
            if (reviewData.containsKey("overallRating")) {
                request.overallRating = new BigDecimal(reviewData.get("overallRating").toString());
            }
            
            // Titre et commentaire
            request.title = reviewData.get("title") != null ? 
                reviewData.get("title").toString() : null;
            request.comment = reviewData.get("comment") != null ? 
                reviewData.get("comment").toString() : null;
            
            // Notes détaillées
            if (reviewData.containsKey("ratings")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> frontendRatings = (Map<String, Object>) reviewData.get("ratings");
                Map<String, BigDecimal> backendRatings = new HashMap<>();
                
                for (Map.Entry<String, Object> entry : frontendRatings.entrySet()) {
                    backendRatings.put(entry.getKey(), new BigDecimal(entry.getValue().toString()));
                }
                request.ratings = backendRatings;
            }

            return reviewService.createReview(request, user);
        } catch (Exception e) {
            LOG.error("Erreur lors de la création d'avis depuis le frontend", e);
            throw e;
        }
    }

    /**
     * Obtenir les avis pour le frontend (compatible avec ReviewsService.js)
     */
    public List<ReviewDto> getReviewsForFrontend(UUID targetId, String category, Map<String, Object> filters) {
        try {
            String status = "approved"; // Par défaut, seulement les avis approuvés
            if (filters != null && filters.containsKey("status")) {
                status = filters.get("status").toString();
            }

            return reviewService.getReviews(targetId, category, status, null);
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des avis pour le frontend", e);
            return List.of();
        }
    }

    /**
     * Obtenir les statistiques d'avis pour le frontend
     */
    public Map<String, Object> getReviewStatsForFrontend(UUID targetId, String category) {
        try {
            ReviewStatsDto stats = reviewService.getReviewStats(targetId, category);
            
            Map<String, Object> frontendStats = new HashMap<>();
            frontendStats.put("totalReviews", stats.totalReviews);
            frontendStats.put("averageRating", stats.averageRating);
            frontendStats.put("ratingDistribution", stats.ratingDistribution);
            frontendStats.put("categoryRatings", stats.categoryRatings);
            frontendStats.put("helpfulReviews", stats.helpfulReviews);
            frontendStats.put("reportedReviews", stats.reportedReviews);
            frontendStats.put("pendingReviews", stats.pendingReviews);
            
            return frontendStats;
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des stats d'avis", e);
            return new HashMap<>();
        }
    }

    /**
     * Marquer un avis comme utile depuis le frontend
     */
    @Transactional
    public boolean markAsHelpfulFromFrontend(Long reviewId, UUID userId) {
        try {
            return reviewService.markAsHelpful(reviewId, userId);
        } catch (Exception e) {
            LOG.error("Erreur lors du marquage utile depuis le frontend", e);
            return false;
        }
    }

    /**
     * Signaler un avis depuis le frontend
     */
    @Transactional
    public boolean reportReviewFromFrontend(Long reviewId, UUID userId) {
        try {
            return reviewService.reportReview(reviewId, userId);
        } catch (Exception e) {
            LOG.error("Erreur lors du signalement depuis le frontend", e);
            return false;
        }
    }
}
