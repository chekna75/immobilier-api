package com.ditsolution.features.rental.dto;

import java.math.BigDecimal;

public class CreateSplitPaymentRequest {
    public Long contractId;
    public BigDecimal totalAmount;
    public Integer depositPercentage;
    public String description;
}
