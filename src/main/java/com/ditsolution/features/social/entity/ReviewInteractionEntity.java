package com.ditsolution.features.social.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_interactions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "user_id", "interaction_type"}))
public class ReviewInteractionEntity extends PanacheEntity {

    @Column(name = "review_id", nullable = false)
    @NotNull
    public Long reviewId;

    @Column(name = "user_id", nullable = false)
    @NotNull
    public UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false)
    @NotNull
    public InteractionType interactionType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", insertable = false, updatable = false)
    public ReviewEntity review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    public com.ditsolution.features.auth.entity.UserEntity user;

    // Enums
    public enum InteractionType {
        HELPFUL("helpful"),
        REPORT("report");

        private final String value;

        InteractionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Méthodes statiques
    public static ReviewInteractionEntity findByReviewAndUser(Long reviewId, UUID userId, InteractionType type) {
        return find("reviewId = ?1 AND userId = ?2 AND interactionType = ?3", 
                   reviewId, userId, type).firstResult();
    }

    public static java.util.List<ReviewInteractionEntity> findByReview(Long reviewId) {
        return find("reviewId = ?1 ORDER BY createdAt DESC", reviewId).list();
    }

    public static java.util.List<ReviewInteractionEntity> findByUser(UUID userId) {
        return find("userId = ?1 ORDER BY createdAt DESC", userId).list();
    }

    public static long countByReviewAndType(Long reviewId, InteractionType type) {
        return count("reviewId = ?1 AND interactionType = ?2", reviewId, type);
    }

    public static boolean hasUserInteracted(Long reviewId, UUID userId, InteractionType type) {
        return count("reviewId = ?1 AND userId = ?2 AND interactionType = ?3", 
                    reviewId, userId, type) > 0;
    }

    // Méthodes d'instance
    public boolean isHelpful() {
        return InteractionType.HELPFUL.equals(this.interactionType);
    }

    public boolean isReport() {
        return InteractionType.REPORT.equals(this.interactionType);
    }
}
