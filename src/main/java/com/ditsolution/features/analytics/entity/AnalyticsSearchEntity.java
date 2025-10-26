package com.ditsolution.features.analytics.entity;

import com.ditsolution.features.auth.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "analytics_searches")
@Data
@EqualsAndHashCode(callSuper = false)
public class AnalyticsSearchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Column(name = "query", length = 500)
    public String query;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters", columnDefinition = "JSONB")
    public Map<String, Object> filters;

    @Column(name = "source", length = 50)
    public String source; // 'app', 'web', 'search', etc.

    @Column(name = "device", length = 50)
    public String device; // 'mobile', 'desktop', 'tablet'

    @Column(name = "location", length = 100)
    public String location;

    @Column(name = "results_count")
    public Integer resultsCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    // Index pour optimiser les requêtes
    @Table(name = "analytics_searches", indexes = {
        @Index(name = "idx_analytics_searches_user", columnList = "user_id"),
        @Index(name = "idx_analytics_searches_query", columnList = "query"),
        @Index(name = "idx_analytics_searches_created_at", columnList = "created_at"),
        @Index(name = "idx_analytics_searches_source", columnList = "source")
    })
    public static class AnalyticsSearchEntityIndexes {}
}
