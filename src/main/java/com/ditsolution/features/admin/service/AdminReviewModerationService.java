package com.ditsolution.features.admin.service;

import com.ditsolution.common.utils.HttpErrors;
import com.ditsolution.features.admin.dto.ModerateReviewRequest;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.auth.service.AdminAuditService;
import com.ditsolution.features.social.entity.ReviewEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class AdminReviewModerationService {

    @Inject
    AdminAuditService auditService;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public ReviewEntity moderateReview(Long reviewId, UserEntity admin, ModerateReviewRequest request, String ip, String userAgent) {
        ReviewEntity review = ReviewEntity.findById(reviewId);
        if (review == null) {
            throw new NotFoundException("Avis non trouvé");
        }

        String action = request.action().toUpperCase();
        
        switch (action) {
            case "APPROVE":
                return approveReview(review, admin, request.reason(), ip, userAgent);
            case "REJECT":
                return rejectReview(review, admin, request.reason(), ip, userAgent);
            default:
                throw HttpErrors.badRequest("INVALID_ACTION", "Action invalide. Utilisez 'APPROVE' ou 'REJECT'");
        }
    }

    private ReviewEntity approveReview(ReviewEntity review, UserEntity admin, String reason, String ip, String userAgent) {
        if (review.status != ReviewEntity.ReviewStatus.PENDING) {
            throw HttpErrors.badRequest("NOT_PENDING", "L'avis n'est pas en attente de modération");
        }

        // Approuver l'avis
        review.approve();
        review.persist();
        
        // Log de l'action
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("newStatus", ReviewEntity.ReviewStatus.APPROVED.name());
        details.put("reviewId", review.id);
        details.put("targetId", review.targetId);
        details.put("targetType", review.targetType.getValue());
        details.put("reviewerId", review.reviewerId);
        details.put("overallRating", review.overallRating);
        
        try {
            auditService.log(
                admin.getId(),
                "REVIEW_APPROVE",
                "REVIEW",
                UUID.fromString(review.id.toString()),
                objectMapper.writeValueAsString(details),
                ip,
                userAgent
            );
        } catch (Exception e) {
            // Fallback si JSON échoue
            auditService.log(
                admin.getId(),
                "REVIEW_APPROVE",
                "REVIEW",
                UUID.fromString(review.id.toString()),
                "reason: " + reason + ", newStatus: " + ReviewEntity.ReviewStatus.APPROVED.name(),
                ip,
                userAgent
            );
        }

        return review;
    }

    private ReviewEntity rejectReview(ReviewEntity review, UserEntity admin, String reason, String ip, String userAgent) {
        if (review.status != ReviewEntity.ReviewStatus.PENDING) {
            throw HttpErrors.badRequest("NOT_PENDING", "L'avis n'est pas en attente de modération");
        }

        // Rejeter l'avis
        review.reject();
        review.persist();
        
        // Log de l'action
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("newStatus", ReviewEntity.ReviewStatus.REJECTED.name());
        details.put("reviewId", review.id);
        details.put("targetId", review.targetId);
        details.put("targetType", review.targetType.getValue());
        details.put("reviewerId", review.reviewerId);
        details.put("overallRating", review.overallRating);
        
        try {
            auditService.log(
                admin.getId(),
                "REVIEW_REJECT",
                "REVIEW",
                UUID.fromString(review.id.toString()),
                objectMapper.writeValueAsString(details),
                ip,
                userAgent
            );
        } catch (Exception e) {
            // Fallback si JSON échoue
            auditService.log(
                admin.getId(),
                "REVIEW_REJECT",
                "REVIEW",
                UUID.fromString(review.id.toString()),
                "reason: " + reason + ", newStatus: " + ReviewEntity.ReviewStatus.REJECTED.name(),
                ip,
                userAgent
            );
        }

        return review;
    }

    public java.util.List<ReviewEntity> getModerationQueue() {
        // Retourner les avis en attente de modération
        return ReviewEntity.findPendingReviews();
    }

    public java.util.List<ReviewEntity> getReportedReviews() {
        // Retourner les avis signalés
        return ReviewEntity.find("reportCount > 0 ORDER BY reportCount DESC, createdAt ASC").list();
    }

    public java.util.List<ReviewEntity> getHighReportedReviews(int threshold) {
        // Retourner les avis avec beaucoup de signalements
        return ReviewEntity.find("reportCount >= ?1 ORDER BY reportCount DESC", threshold).list();
    }
}
