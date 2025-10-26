package com.ditsolution.features.analytics.dto;

import com.ditsolution.features.analytics.entity.AnalyticsFavoriteEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class AnalyticsFavoriteDto {
    
    @NotNull
    @JsonProperty("listing_id")
    public UUID listingId;
    
    @NotNull
    @JsonProperty("user_id")
    public UUID userId;
    
    @NotNull
    public AnalyticsFavoriteEntity.FavoriteAction action;
    
    public String source;
    public String device;
    
    @JsonProperty("created_at")
    public LocalDateTime createdAt;
}
