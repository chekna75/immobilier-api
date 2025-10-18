package com.ditsolution.features.listing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ditsolution.features.listing.enums.ListingStatus;
import com.ditsolution.features.listing.enums.ListingType;

public record ListingDto(
    UUID id,
    UUID ownerId,
    ListingStatus status,
    ListingType type,
    String city,
    String district,
    BigDecimal price,
    String title,
    String description,
    
    // Géolocalisation
    BigDecimal latitude,
    BigDecimal longitude,
    
    // Champs enrichis
    Integer rooms,
    Integer floor,
    Integer buildingYear,
    String energyClass,
    Boolean hasElevator,
    Boolean hasParking,
    Boolean hasBalcony,
    Boolean hasTerrace,
    
    List<String> photos,
    Instant createdAt,
    Instant updatedAt
) {}