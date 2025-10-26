package com.ditsolution.features.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AnalyticsReportDto {
    
    public UUID id;
    
    @JsonProperty("owner_id")
    public UUID ownerId;
    
    @JsonProperty("listing_id")
    public UUID listingId;
    
    public String period;
    
    @JsonProperty("report_type")
    public String reportType;
    
    @JsonProperty("generated_at")
    public LocalDateTime generatedAt;
    
    public AnalyticsMetricsDto metrics;
    public AnalyticsBreakdownDto breakdown;
    public AnalyticsTimeSeriesDto timeSeries;
    public List<AnalyticsRecommendationDto> recommendations;
    public AnalyticsSummaryDto summary;
    public Map<String, Object> charts;
}

