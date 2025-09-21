package com.ditsolution.features.listing.dto;

import java.math.BigDecimal;
import java.util.List;

import com.ditsolution.features.listing.enums.ListingType;

public record ListingCreateDto(
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
    
    List<String> photos // URLs ou chemins uploadés
) {}
