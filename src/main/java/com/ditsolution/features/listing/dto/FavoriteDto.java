package com.ditsolution.features.listing.dto;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class FavoriteDto {
    private Long id;
    private UUID userId;
    private UUID listingId;
    private Instant createdAt;
    
    // Informations du bien pour l'affichage
    private String listingTitle;
    private String listingCity;
    private String listingDistrict;
    private String listingPrice;
    private String listingThumbnailUrl;
}
