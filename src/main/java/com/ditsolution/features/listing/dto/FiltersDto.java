package com.ditsolution.features.listing.dto;

import java.math.BigDecimal;

import com.ditsolution.features.listing.enums.ListingType;

public record FiltersDto(
            String city, String district, ListingType type,
            BigDecimal minPrice, BigDecimal maxPrice) {}