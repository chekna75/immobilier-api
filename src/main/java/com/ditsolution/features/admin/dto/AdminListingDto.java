package com.ditsolution.features.admin.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminListingDto(
    UUID id,
    String title,
    String description,
    String city,
    String district,
    BigDecimal price,
    String type,
    String status,
    boolean isPremium,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    
    // Informations propriétaire
    UUID ownerId,
    String ownerEmail,
    String ownerFirstName,
    String ownerLastName,
    
    // Photos
    List<String> photoUrls,
    
    // Détails enrichis
    Integer rooms,
    Integer floor,
    Integer buildingYear,
    String energyClass,
    Boolean hasElevator,
    Boolean hasParking,
    Boolean hasBalcony,
    Boolean hasTerrace,
    BigDecimal latitude,
    BigDecimal longitude
) {}
