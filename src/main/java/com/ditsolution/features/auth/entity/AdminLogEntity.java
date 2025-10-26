package com.ditsolution.features.auth.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "admin_logs")
public class AdminLogEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "admin_id", nullable = false)
    public UUID adminId;

    @Column(nullable = false, length = 50)
    public String action;

    @Column(name = "target_type", nullable = false, length = 50)
    public String targetType;

    @Column(name = "target_id", nullable = false)
    public UUID targetId;

    @Column(columnDefinition = "JSONB")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    public JsonNode details;

    @Column(length = 45)
    public String ip;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    public String userAgent;

    @Column(name = "created_at", updatable = false)
    public OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
