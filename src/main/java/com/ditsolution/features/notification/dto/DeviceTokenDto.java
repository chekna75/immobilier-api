package com.ditsolution.features.notification.dto;

import com.ditsolution.features.notification.entity.DeviceTokenEntity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DeviceTokenDto {
    private UUID id;
    private UUID userId;
    private String token;
    private DeviceTokenEntity.Platform platform;
    private String appVersion;
    private String deviceModel;
    private Boolean isActive;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DeviceTokenDto() {}

    public DeviceTokenDto(DeviceTokenEntity entity) {
        this.id = entity.getId();
        this.userId = entity.getUser().getId();
        this.token = entity.getToken();
        this.platform = entity.getPlatform();
        this.appVersion = entity.getAppVersion();
        this.deviceModel = entity.getDeviceModel();
        this.isActive = entity.getIsActive();
        this.lastUsedAt = entity.getLastUsedAt();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }
}

