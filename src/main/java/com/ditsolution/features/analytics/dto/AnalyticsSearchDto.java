package com.ditsolution.features.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class AnalyticsSearchDto {
    
    @JsonProperty("user_id")
    public UUID userId;
    
    @NotNull
    public String query;
    
    public Map<String, Object> filters;
    public String source;
    public String device;
    public String location;
    
    @JsonProperty("results_count")
    public Integer resultsCount;
    
    @JsonProperty("created_at")
    public LocalDateTime createdAt;
}
