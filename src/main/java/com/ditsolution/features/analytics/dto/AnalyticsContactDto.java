package com.ditsolution.features.analytics.dto;

import com.ditsolution.features.analytics.entity.AnalyticsContactEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class AnalyticsContactDto {
    
    @NotNull
    @JsonProperty("listing_id")
    public UUID listingId;
    
    @NotNull
    @JsonProperty("user_id")
    public UUID userId;
    
    @NotNull
    @JsonProperty("contact_type")
    public AnalyticsContactEntity.ContactType contactType;
    
    public String source;
    public String device;
    public String message;
    
    @JsonProperty("created_at")
    public LocalDateTime createdAt;
}
