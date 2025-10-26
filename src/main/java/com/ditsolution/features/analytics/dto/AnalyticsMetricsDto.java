package com.ditsolution.features.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnalyticsMetricsDto {
    @JsonProperty("total_views")
    public Long totalViews;
    
    @JsonProperty("unique_views")
    public Long uniqueViews;
    
    @JsonProperty("total_clicks")
    public Long totalClicks;
    
    @JsonProperty("total_favorites")
    public Long totalFavorites;
    
    @JsonProperty("total_contacts")
    public Long totalContacts;
    
    @JsonProperty("total_conversions")
    public Long totalConversions;
    
    @JsonProperty("conversion_rate")
    public Double conversionRate;
    
    @JsonProperty("click_through_rate")
    public Double clickThroughRate;
    
    @JsonProperty("favorite_rate")
    public Double favoriteRate;
    
    @JsonProperty("contact_rate")
    public Double contactRate;
}
