package com.ditsolution.features.analytics.dto;

import com.ditsolution.features.analytics.entity.AnalyticsConversionEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AnalyticsConversionDto {
    
    @NotNull
    @JsonProperty("listing_id")
    public UUID listingId;
    
    @NotNull
    @JsonProperty("user_id")
    public UUID userId;
    
    @NotNull
    @JsonProperty("conversion_type")
    public AnalyticsConversionEntity.ConversionType conversionType;
    
    public String source;
    public String device;
    public BigDecimal value;
    
    @NotNull
    public AnalyticsConversionEntity.ConversionStatus status;
    
    @JsonProperty("created_at")
    public LocalDateTime createdAt;
}
