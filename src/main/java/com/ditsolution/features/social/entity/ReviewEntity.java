package com.ditsolution.features.social.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"target_id", "target_type", "reviewer_id"}))
public class ReviewEntity extends PanacheEntity {

    @Column(name = "target_id", nullable = false)
    @NotNull
    public UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    @NotNull
    public TargetType targetType;

    @Column(name = "reviewer_id", nullable = false)
    @NotNull
    public UUID reviewerId;

    @Column(name = "overall_rating", nullable = false, precision = 2, scale = 1)
    @NotNull
    @DecimalMin(value = "1.0", message = "La note doit être au minimum 1")
    @DecimalMax(value = "5.0", message = "La note doit être au maximum 5")
    public BigDecimal overallRating;

    @Column(name = "title", length = 255)
    @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères")
    public String title;

    @Column(name = "comment", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères")
    public String comment;

    @Column(name = "ratings", columnDefinition = "JSONB", nullable = false)
    @NotNull
    public String ratings; // JSON string des critères détaillés

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    public ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "helpful_count", nullable = false)
    @Min(value = 0, message = "Le nombre d'avis utiles ne peut pas être négatif")
    public Integer helpfulCount = 0;

    @Column(name = "report_count", nullable = false)
    @Min(value = 0, message = "Le nombre de signalements ne peut pas être négatif")
    public Integer reportCount = 0;

    @Column(name = "is_verified", nullable = false)
    public Boolean isVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", insertable = false, updatable = false)
    public com.ditsolution.features.auth.entity.UserEntity reviewer;

    // Enums
    public enum TargetType {
        PROPERTY("property"),
        OWNER("owner"),
        AGENCY("agency");

        private final String value;

        TargetType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum ReviewStatus {
        PENDING("pending"),
        APPROVED("approved"),
        REJECTED("rejected");

        private final String value;

        ReviewStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Méthodes utilitaires
    public static ReviewEntity findByTarget(UUID targetId, TargetType targetType) {
        return find("targetId = ?1 AND targetType = ?2", targetId, targetType).firstResult();
    }

    public static java.util.List<ReviewEntity> findByTargetAndStatus(UUID targetId, TargetType targetType, ReviewStatus status) {
        return find("targetId = ?1 AND targetType = ?2 AND status = ?3", targetId, targetType, status).list();
    }

    public static java.util.List<ReviewEntity> findByReviewer(UUID reviewerId) {
        return find("reviewerId = ?1 ORDER BY createdAt DESC", reviewerId).list();
    }

    public static java.util.List<ReviewEntity> findPendingReviews() {
        return find("status = ?1 ORDER BY createdAt ASC", ReviewStatus.PENDING).list();
    }

    public static long countByTarget(UUID targetId, TargetType targetType) {
        return count("targetId = ?1 AND targetType = ?2 AND status = ?3", 
                    targetId, targetType, ReviewStatus.APPROVED);
    }

    public static BigDecimal getAverageRating(UUID targetId, TargetType targetType) {
        var result = getEntityManager()
            .createQuery("SELECT AVG(r.overallRating) FROM ReviewEntity r WHERE r.targetId = ?1 AND r.targetType = ?2 AND r.status = ?3", 
                        BigDecimal.class)
            .setParameter(1, targetId)
            .setParameter(2, targetType)
            .setParameter(3, ReviewStatus.APPROVED)
            .getSingleResult();
        
        return result != null ? result : BigDecimal.ZERO;
    }

    // Méthodes d'instance
    public void incrementHelpfulCount() {
        this.helpfulCount++;
    }

    public void incrementReportCount() {
        this.reportCount++;
    }

    public void approve() {
        this.status = ReviewStatus.APPROVED;
    }

    public void reject() {
        this.status = ReviewStatus.REJECTED;
    }

    public boolean isApproved() {
        return ReviewStatus.APPROVED.equals(this.status);
    }

    public boolean isPending() {
        return ReviewStatus.PENDING.equals(this.status);
    }

    public boolean isRejected() {
        return ReviewStatus.REJECTED.equals(this.status);
    }
}
