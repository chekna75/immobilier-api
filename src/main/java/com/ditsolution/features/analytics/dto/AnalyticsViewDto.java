package com.ditsolution.features.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class AnalyticsViewDto {
    
    @NotNull
    @JsonProperty("listing_id")
    public UUID listingId;
    
    @JsonProperty("user_id")
    public UUID userId;
    
    public String source;
    public String device;
    public String location;
    
    @JsonProperty("user_agent")
    public String userAgent;
    
    @JsonProperty("session_id")
    public String sessionId;
    
    public String referrer;
    
    @JsonProperty("created_at")
    public LocalDateTime createdAt;
}
