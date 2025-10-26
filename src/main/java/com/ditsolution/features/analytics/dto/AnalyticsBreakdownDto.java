package com.ditsolution.features.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AnalyticsBreakdownDto {
    @JsonProperty("views_by_source")
    public Map<String, Long> viewsBySource;
    
    @JsonProperty("views_by_device")
    public Map<String, Long> viewsByDevice;
    
    @JsonProperty("clicks_by_action")
    public Map<String, Long> clicksByAction;
    
    @JsonProperty("contacts_by_type")
    public Map<String, Long> contactsByType;
}
