package com.ditsolution.features.social.dto;

import java.math.BigDecimal;
import java.util.Map;

public class ReviewStatsDto {
    public Long totalReviews;
    public BigDecimal averageRating;
    public Map<Integer, Long> ratingDistribution;
    public Map<String, BigDecimal> categoryRatings;
    public Long helpfulReviews;
    public Long reportedReviews;
    public Long pendingReviews;

    public ReviewStatsDto() {}

    public ReviewStatsDto(Long totalReviews, BigDecimal averageRating, 
                         Map<Integer, Long> ratingDistribution, 
                         Map<String, BigDecimal> categoryRatings,
                         Long helpfulReviews, Long reportedReviews, Long pendingReviews) {
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
        this.ratingDistribution = ratingDistribution;
        this.categoryRatings = categoryRatings;
        this.helpfulReviews = helpfulReviews;
        this.reportedReviews = reportedReviews;
        this.pendingReviews = pendingReviews;
    }
}
