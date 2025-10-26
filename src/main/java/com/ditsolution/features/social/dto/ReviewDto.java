package com.ditsolution.features.social.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class ReviewDto {
    public UUID id;
    public UUID targetId;
    public String targetType;
    public UUID reviewerId;
    public String reviewerName;
    public String reviewerAvatar;
    public BigDecimal overallRating;
    public String title;
    public String comment;
    public Map<String, BigDecimal> ratings;
    public String status;
    public Integer helpfulCount;
    public Integer reportCount;
    public Boolean isVerified;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
    public Boolean hasUserMarkedHelpful;
    public Boolean hasUserReported;

    public ReviewDto() {}

    public ReviewDto(UUID id, UUID targetId, String targetType, UUID reviewerId, 
                    String reviewerName, String reviewerAvatar, BigDecimal overallRating, 
                    String title, String comment, Map<String, BigDecimal> ratings, 
                    String status, Integer helpfulCount, Integer reportCount, 
                    Boolean isVerified, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.targetId = targetId;
        this.targetType = targetType;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewerAvatar = reviewerAvatar;
        this.overallRating = overallRating;
        this.title = title;
        this.comment = comment;
        this.ratings = ratings;
        this.status = status;
        this.helpfulCount = helpfulCount;
        this.reportCount = reportCount;
        this.isVerified = isVerified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
