package com.ditsolution.features.analytics.dto;

import java.util.List;

public class AnalyticsTimeSeriesDto {
    public List<AnalyticsDataPointDto> views;
    public List<AnalyticsDataPointDto> clicks;
    public List<AnalyticsDataPointDto> favorites;
    public List<AnalyticsDataPointDto> contacts;
    public List<AnalyticsDataPointDto> conversions;
}
