package com.ditsolution.features.social.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "social_shares")
public class SocialShareEntity extends PanacheEntity {

    @Column(name = "listing_id")
    public UUID listingId;

    @Column(name = "user_id", nullable = false)
    @NotNull
    public UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    @NotNull
    public SocialPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "share_type", nullable = false)
    @NotNull
    public ShareType shareType;

    @CreationTimestamp
    @Column(name = "shared_at", nullable = false)
    public OffsetDateTime sharedAt;

    @Column(name = "metadata", columnDefinition = "JSONB")
    public String metadata; // JSON string pour données additionnelles

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    public com.ditsolution.features.listing.entity.ListingEntity listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    public com.ditsolution.features.auth.entity.UserEntity user;

    // Enums
    public enum SocialPlatform {
        FACEBOOK("facebook"),
        TWITTER("twitter"),
        INSTAGRAM("instagram"),
        LINKEDIN("linkedin"),
        WHATSAPP("whatsapp"),
        TELEGRAM("telegram"),
        NATIVE("native");

        private final String value;

        SocialPlatform(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum ShareType {
        PROPERTY("property"),
        FAVORITE("favorite"),
        SEARCH("search");

        private final String value;

        ShareType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Méthodes statiques pour les requêtes
    public static java.util.List<SocialShareEntity> findByListing(UUID listingId) {
        return find("listingId = ?1 ORDER BY sharedAt DESC", listingId).list();
    }

    public static java.util.List<SocialShareEntity> findByUser(UUID userId) {
        return find("userId = ?1 ORDER BY sharedAt DESC", userId).list();
    }

    public static java.util.List<SocialShareEntity> findByPlatform(SocialPlatform platform) {
        return find("platform = ?1 ORDER BY sharedAt DESC", platform).list();
    }

    public static java.util.List<SocialShareEntity> findByShareType(ShareType shareType) {
        return find("shareType = ?1 ORDER BY sharedAt DESC", shareType).list();
    }

    public static long countByListing(UUID listingId) {
        return count("listingId = ?1", listingId);
    }

    public static long countByUser(UUID userId) {
        return count("userId = ?1", userId);
    }

    public static long countByPlatform(SocialPlatform platform) {
        return count("platform = ?1", platform);
    }

    public static long countByShareType(ShareType shareType) {
        return count("shareType = ?1", shareType);
    }

    public static java.util.List<SocialShareEntity> findRecentShares(int limit) {
        return find("ORDER BY sharedAt DESC").page(0, limit).list();
    }

    public static java.util.List<SocialShareEntity> findByDateRange(OffsetDateTime startDate, OffsetDateTime endDate) {
        return find("sharedAt BETWEEN ?1 AND ?2 ORDER BY sharedAt DESC", startDate, endDate).list();
    }

    // Méthodes d'instance
    public boolean isPropertyShare() {
        return ShareType.PROPERTY.equals(this.shareType);
    }

    public boolean isFavoriteShare() {
        return ShareType.FAVORITE.equals(this.shareType);
    }

    public boolean isSearchShare() {
        return ShareType.SEARCH.equals(this.shareType);
    }

    public boolean isNativeShare() {
        return SocialPlatform.NATIVE.equals(this.platform);
    }

    public boolean isSocialMediaShare() {
        return !SocialPlatform.NATIVE.equals(this.platform);
    }
}
