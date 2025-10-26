package com.ditsolution.features.analytics.dto;

import com.ditsolution.features.analytics.entity.AnalyticsClickEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class AnalyticsClickDto {
    
    @NotNull
    @JsonProperty("listing_id")
    public UUID listingId;
    
    @JsonProperty("user_id")
    public UUID userId;
    
    @NotNull
    public AnalyticsClickEntity.ClickAction action;
    
    public String source;
    public String device;
    public String location;
    
    @JsonProperty("session_id")
    public String sessionId;
    
    @JsonProperty("created_at")
    public LocalDateTime createdAt;
}
