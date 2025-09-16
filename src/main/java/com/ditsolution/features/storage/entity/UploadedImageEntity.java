package com.ditsolution.features.storage.entity;

import com.ditsolution.features.auth.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "uploaded_images")
@Data
@EqualsAndHashCode(callSuper = true)
public class UploadedImageEntity extends PanacheEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "s3_key", nullable = false, length = 1000)
    private String s3Key;

    @Column(name = "public_url", nullable = false, length = 1000)
    private String publicUrl;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Méthodes utilitaires
    public static UploadedImageEntity findByUserIdAndFileName(UUID userId, String fileName) {
        return find("userId = ?1 and fileName = ?2", userId, fileName).firstResult();
    }

    public static java.util.List<UploadedImageEntity> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public static java.util.List<UploadedImageEntity> findUnusedByUserId(UUID userId) {
        return find("userId = ?1 and isUsed = ?2", userId, false).list();
    }
}
