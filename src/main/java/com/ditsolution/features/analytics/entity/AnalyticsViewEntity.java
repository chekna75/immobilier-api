package com.ditsolution.features.analytics.entity;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics_views")
@Data
@EqualsAndHashCode(callSuper = false)
public class AnalyticsViewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    public ListingEntity listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Column(name = "source", length = 50)
    public String source; // 'app', 'web', 'search', etc.

    @Column(name = "device", length = 50)
    public String device; // 'mobile', 'desktop', 'tablet'

    @Column(name = "location", length = 100)
    public String location;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    public String userAgent;

    @Column(name = "session_id", length = 100)
    public String sessionId;

    @Column(name = "referrer", length = 500)
    public String referrer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    // Index pour optimiser les requêtes
    @Table(name = "analytics_views", indexes = {
        @Index(name = "idx_analytics_views_listing", columnList = "listing_id"),
        @Index(name = "idx_analytics_views_user", columnList = "user_id"),
        @Index(name = "idx_analytics_views_created_at", columnList = "created_at"),
        @Index(name = "idx_analytics_views_source", columnList = "source"),
        @Index(name = "idx_analytics_views_device", columnList = "device")
    })
    public static class AnalyticsViewEntityIndexes {}
}
