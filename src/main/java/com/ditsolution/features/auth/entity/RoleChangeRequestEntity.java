package com.ditsolution.features.auth.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Entity
@Table(name = "role_change_requests")
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleChangeRequestEntity extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "requested_role", nullable = false)
    private String requestedRole;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private UserEntity processedBy;
}
