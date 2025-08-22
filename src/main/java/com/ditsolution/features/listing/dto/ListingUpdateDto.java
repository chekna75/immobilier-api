package com.ditsolution.features.listing.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.ditsolution.features.listing.enums.ListingType;

public record ListingUpdateDto(
    Optional<ListingType> type,
    Optional<String> city,
    Optional<String> district,
    Optional<BigDecimal> price,
    Optional<String> title,
    Optional<String> description,
    Optional<List<String>> photos
) {}
