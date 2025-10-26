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
@Table(name = "analytics_favorites")
@Data
@EqualsAndHashCode(callSuper = false)
public class AnalyticsFavoriteEntity {

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
    @Column(name = "action", nullable = false, length = 20)
    public FavoriteAction action;

    @Column(name = "source", length = 50)
    public String source; // 'app', 'web', 'search', etc.

    @Column(name = "device", length = 50)
    public String device; // 'mobile', 'desktop', 'tablet'

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    public enum FavoriteAction {
        ADD, REMOVE
    }

    // Index pour optimiser les requêtes
    @Table(name = "analytics_favorites", indexes = {
        @Index(name = "idx_analytics_favorites_listing", columnList = "listing_id"),
        @Index(name = "idx_analytics_favorites_user", columnList = "user_id"),
        @Index(name = "idx_analytics_favorites_action", columnList = "action"),
        @Index(name = "idx_analytics_favorites_created_at", columnList = "created_at")
    })
    public static class AnalyticsFavoriteEntityIndexes {}
}
