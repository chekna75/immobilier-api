package com.ditsolution.features.analytics.entity;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics_conversions")
@Data
@EqualsAndHashCode(callSuper = false)
public class AnalyticsConversionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    public ListingEntity listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversion_type", nullable = false, length = 50)
    public ConversionType conversionType;

    @Column(name = "source", length = 50)
    public String source; // 'app', 'web', 'search', etc.

    @Column(name = "device", length = 50)
    public String device; // 'mobile', 'desktop', 'tablet'

    @Column(name = "value", precision = 10, scale = 2)
    public BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    public ConversionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    public enum ConversionType {
        BOOKING, RENTAL, SALE, VISIT, INQUIRY
    }

    public enum ConversionStatus {
        PENDING, COMPLETED, CANCELLED, FAILED
    }

    // Index pour optimiser les requêtes
    @Table(name = "analytics_conversions", indexes = {
        @Index(name = "idx_analytics_conversions_listing", columnList = "listing_id"),
        @Index(name = "idx_analytics_conversions_user", columnList = "user_id"),
        @Index(name = "idx_analytics_conversions_type", columnList = "conversion_type"),
        @Index(name = "idx_analytics_conversions_status", columnList = "status"),
        @Index(name = "idx_analytics_conversions_created_at", columnList = "created_at")
    })
    public static class AnalyticsConversionEntityIndexes {}
}
