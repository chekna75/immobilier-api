package com.ditsolution.features.social.service;

import com.ditsolution.features.social.dto.*;
import com.ditsolution.features.social.entity.*;
import com.ditsolution.features.auth.entity.UserEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.BadRequestException;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ReviewService {

    private static final Logger LOG = Logger.getLogger(ReviewService.class);

    @Inject
    ObjectMapper objectMapper;

    /**
     * Créer un nouvel avis
     */
    @Transactional
    public ReviewDto createReview(CreateReviewRequest request, UserEntity reviewer) {
        try {
            // Vérifier si l'utilisateur a déjà laissé un avis pour cette cible
            ReviewEntity existingReview = ReviewEntity.findByTarget(request.targetId, 
                ReviewEntity.TargetType.valueOf(request.targetType.toUpperCase()));
            
            if (existingReview != null && existingReview.reviewerId.equals(reviewer.getId())) {
                throw new BadRequestException("Vous avez déjà laissé un avis pour cette cible");
            }

            // Créer l'entité
            ReviewEntity review = new ReviewEntity();
            review.targetId = request.targetId;
            review.targetType = ReviewEntity.TargetType.valueOf(request.targetType.toUpperCase());
            review.reviewerId = reviewer.getId();
            review.overallRating = request.overallRating;
            review.title = request.title;
            review.comment = request.comment;
            review.ratings = objectMapper.writeValueAsString(request.ratings);
            review.status = ReviewEntity.ReviewStatus.PENDING;
            review.helpfulCount = 0;
            review.reportCount = 0;
            review.isVerified = false;

            review.persist();

            LOG.info("Avis créé avec succès: " + review.id + " par " + reviewer.getId());

            return mapToDto(review, reviewer.getId());
        } catch (JsonProcessingException e) {
            LOG.error("Erreur lors de la sérialisation des notes", e);
            throw new BadRequestException("Format des notes invalide");
        } catch (Exception e) {
            LOG.error("Erreur lors de la création de l'avis", e);
            throw new BadRequestException("Impossible de créer l'avis: " + e.getMessage());
        }
    }

    /**
     * Récupérer les avis d'une cible
     */
    public List<ReviewDto> getReviews(UUID targetId, String targetType, String status, UUID currentUserId) {
        try {
            ReviewEntity.TargetType type = ReviewEntity.TargetType.valueOf(targetType.toUpperCase());
            List<ReviewEntity> reviews;

            if (status != null && !status.isEmpty()) {
                ReviewEntity.ReviewStatus reviewStatus = ReviewEntity.ReviewStatus.valueOf(status.toUpperCase());
                reviews = ReviewEntity.findByTargetAndStatus(targetId, type, reviewStatus);
            } else {
                reviews = ReviewEntity.findByTargetAndStatus(targetId, type, ReviewEntity.ReviewStatus.APPROVED);
            }

            return reviews.stream()
                .map(review -> mapToDto(review, currentUserId))
                .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des avis", e);
            throw new BadRequestException("Impossible de récupérer les avis");
        }
    }

    /**
     * Marquer un avis comme utile
     */
    @Transactional
    public boolean markAsHelpful(UUID reviewId, UUID userId) {
        try {
            ReviewEntity review = ReviewEntity.findById(reviewId);
            if (review == null) {
                throw new NotFoundException("Avis non trouvé");
            }

            // Vérifier si l'utilisateur a déjà marqué cet avis comme utile
            ReviewInteractionEntity existingInteraction = ReviewInteractionEntity
                .findByReviewAndUser(reviewId, userId, ReviewInteractionEntity.InteractionType.HELPFUL);

            if (existingInteraction != null) {
                // Retirer le marquage
                existingInteraction.delete();
                review.helpfulCount = Math.max(0, review.helpfulCount - 1);
                review.persist();
                return false;
            } else {
                // Ajouter le marquage
                ReviewInteractionEntity interaction = new ReviewInteractionEntity();
                interaction.reviewId = reviewId;
                interaction.userId = userId;
                interaction.interactionType = ReviewInteractionEntity.InteractionType.HELPFUL;
                interaction.persist();

                review.incrementHelpfulCount();
                review.persist();
                return true;
            }
        } catch (Exception e) {
            LOG.error("Erreur lors du marquage de l'avis", e);
            throw new BadRequestException("Impossible de marquer l'avis comme utile");
        }
    }

    /**
     * Signaler un avis
     */
    @Transactional
    public boolean reportReview(UUID reviewId, UUID userId) {
        try {
            ReviewEntity review = ReviewEntity.findById(reviewId);
            if (review == null) {
                throw new NotFoundException("Avis non trouvé");
            }

            // Vérifier si l'utilisateur a déjà signalé cet avis
            ReviewInteractionEntity existingInteraction = ReviewInteractionEntity
                .findByReviewAndUser(reviewId, userId, ReviewInteractionEntity.InteractionType.REPORT);

            if (existingInteraction != null) {
                return false; // Déjà signalé
            }

            // Créer le signalement
            ReviewInteractionEntity interaction = new ReviewInteractionEntity();
            interaction.reviewId = reviewId;
            interaction.userId = userId;
            interaction.interactionType = ReviewInteractionEntity.InteractionType.REPORT;
            interaction.persist();

            review.incrementReportCount();
            review.persist();

            LOG.info("Avis signalé: " + reviewId + " par " + userId);
            return true;
        } catch (Exception e) {
            LOG.error("Erreur lors du signalement de l'avis", e);
            throw new BadRequestException("Impossible de signaler l'avis");
        }
    }

    /**
     * Obtenir les statistiques d'avis
     */
    public ReviewStatsDto getReviewStats(UUID targetId, String targetType) {
        try {
            ReviewEntity.TargetType type = ReviewEntity.TargetType.valueOf(targetType.toUpperCase());
            
            // Avis approuvés uniquement
            List<ReviewEntity> approvedReviews = ReviewEntity.findByTargetAndStatus(
                targetId, type, ReviewEntity.ReviewStatus.APPROVED);

            if (approvedReviews.isEmpty()) {
                return new ReviewStatsDto(0L, BigDecimal.ZERO, new HashMap<>(), 
                    new HashMap<>(), 0L, 0L, 0L);
            }

            // Calculs des statistiques
            Long totalReviews = (long) approvedReviews.size();
            BigDecimal averageRating = approvedReviews.stream()
                .map(r -> r.overallRating)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(totalReviews), 2, java.math.RoundingMode.HALF_UP);

            // Distribution des notes
            Map<Integer, Long> ratingDistribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                final int rating = i;
                long count = approvedReviews.stream()
                    .mapToLong(r -> Math.round(r.overallRating.doubleValue()) == rating ? 1 : 0)
                    .sum();
                ratingDistribution.put(i, count);
            }

            // Notes par catégorie (simplifié)
            Map<String, BigDecimal> categoryRatings = new HashMap<>();
            // TODO: Implémenter le calcul des notes par catégorie basé sur le JSON ratings

            // Compteurs
            Long helpfulReviews = approvedReviews.stream()
                .mapToLong(r -> r.helpfulCount)
                .sum();
            Long reportedReviews = approvedReviews.stream()
                .mapToLong(r -> r.reportCount)
                .sum();

            // Avis en attente
            Long pendingReviews = ReviewEntity.count("targetId = ?1 AND targetType = ?2 AND status = ?3",
                targetId, type, ReviewEntity.ReviewStatus.PENDING);

            return new ReviewStatsDto(totalReviews, averageRating, ratingDistribution, 
                categoryRatings, helpfulReviews, reportedReviews, pendingReviews);
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des statistiques", e);
            throw new BadRequestException("Impossible de calculer les statistiques");
        }
    }

    /**
     * Mapper l'entité vers le DTO
     */
    private ReviewDto mapToDto(ReviewEntity review, UUID currentUserId) {
        try {
            Map<String, BigDecimal> ratings = objectMapper.readValue(review.ratings, 
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, BigDecimal.class));

            ReviewDto dto = new ReviewDto();
            dto.id = review.id;
            dto.targetId = review.targetId;
            dto.targetType = review.targetType.getValue();
            dto.reviewerId = review.reviewerId;
            dto.overallRating = review.overallRating;
            dto.title = review.title;
            dto.comment = review.comment;
            dto.ratings = ratings;
            dto.status = review.status.getValue();
            dto.helpfulCount = review.helpfulCount;
            dto.reportCount = review.reportCount;
            dto.isVerified = review.isVerified;
            dto.createdAt = review.createdAt;
            dto.updatedAt = review.updatedAt;

            // Vérifier les interactions de l'utilisateur actuel
            if (currentUserId != null) {
                dto.hasUserMarkedHelpful = ReviewInteractionEntity.hasUserInteracted(
                    review.id, currentUserId, ReviewInteractionEntity.InteractionType.HELPFUL);
                dto.hasUserReported = ReviewInteractionEntity.hasUserInteracted(
                    review.id, currentUserId, ReviewInteractionEntity.InteractionType.REPORT);
            }

            return dto;
        } catch (Exception e) {
            LOG.error("Erreur lors du mapping de l'avis", e);
            throw new BadRequestException("Erreur lors du traitement de l'avis");
        }
    }
}
