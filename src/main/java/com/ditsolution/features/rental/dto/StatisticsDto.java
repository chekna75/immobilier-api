package com.ditsolution.features.rental.dto;

import java.math.BigDecimal;
import java.util.List;

public class StatisticsDto {
    public BigDecimal totalIncome;
    public BigDecimal averageMonthlyIncome;
    public Double incomeTrend;
    public Double occupancyRate;
    public Double occupancyTrend;
    public Double onTimePayments;
    public Double punctualityTrend;
    public Double averageDelay;
    public List<PropertyStatisticsDto> properties;
    public String bestMonth;
    public String worstMonth;
}
