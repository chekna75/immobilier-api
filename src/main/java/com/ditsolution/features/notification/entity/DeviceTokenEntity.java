package com.ditsolution.features.notification.entity;

import com.ditsolution.features.auth.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
@Data
@EqualsAndHashCode(callSuper = false)
public class DeviceTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "token", nullable = false, length = 1000)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private Platform platform;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "device_model")
    private String deviceModel;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Platform {
        ANDROID,
        IOS,
        WEB
    }

    // Constructeurs
    public DeviceTokenEntity() {}

    public DeviceTokenEntity(UserEntity user, String token, Platform platform) {
        this.user = user;
        this.token = token;
        this.platform = platform;
        this.lastUsedAt = LocalDateTime.now();
    }
}

