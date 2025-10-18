package com.ditsolution.features.notification.dto;

import com.ditsolution.features.notification.entity.DeviceTokenEntity;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class RegisterDeviceTokenRequest {
    
    @NotBlank(message = "Le token FCM est requis")
    private String token;
    
    @NotNull(message = "La plateforme est requise")
    private DeviceTokenEntity.Platform platform;
    
    private String appVersion;
    private String deviceModel;
}

