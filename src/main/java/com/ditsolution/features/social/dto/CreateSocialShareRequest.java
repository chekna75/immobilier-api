package com.ditsolution.features.social.dto;

import jakarta.validation.constraints.*;
import java.util.Map;
import java.util.UUID;

public class CreateSocialShareRequest {
    public UUID listingId;

    @NotBlank(message = "La plateforme est requise")
    @Pattern(regexp = "^(facebook|twitter|instagram|linkedin|whatsapp|telegram|native)$", 
             message = "Plateforme invalide")
    public String platform;

    @NotBlank(message = "Le type de partage est requis")
    @Pattern(regexp = "^(property|favorite|search)$", message = "Type de partage invalide")
    public String shareType;

    public Map<String, Object> metadata;

    public CreateSocialShareRequest() {}

    public CreateSocialShareRequest(UUID listingId, String platform, String shareType, 
                                   Map<String, Object> metadata) {
        this.listingId = listingId;
        this.platform = platform;
        this.shareType = shareType;
        this.metadata = metadata;
    }
}
