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
@Table(name = "analytics_clicks")
@Data
@EqualsAndHashCode(callSuper = false)
public class AnalyticsClickEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    public ListingEntity listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    public ClickAction action;

    @Column(name = "source", length = 50)
    public String source; // 'app', 'web', 'search', etc.

    @Column(name = "device", length = 50)
    public String device; // 'mobile', 'desktop', 'tablet'

    @Column(name = "location", length = 100)
    public String location;

    @Column(name = "session_id", length = 100)
    public String sessionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    public enum ClickAction {
        VIEW, CONTACT, FAVORITE, SHARE, CALL, EMAIL, VISIT, BOOK
    }

    // Index pour optimiser les requêtes
    @Table(name = "analytics_clicks", indexes = {
        @Index(name = "idx_analytics_clicks_listing", columnList = "listing_id"),
        @Index(name = "idx_analytics_clicks_user", columnList = "user_id"),
        @Index(name = "idx_analytics_clicks_action", columnList = "action"),
        @Index(name = "idx_analytics_clicks_created_at", columnList = "created_at"),
        @Index(name = "idx_analytics_clicks_source", columnList = "source")
    })
    public static class AnalyticsClickEntityIndexes {}
}
