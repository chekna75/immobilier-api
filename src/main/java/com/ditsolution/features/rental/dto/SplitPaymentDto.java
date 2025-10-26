package com.ditsolution.features.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SplitPaymentDto {
    public Long id;
    public Long contractId;
    public BigDecimal totalAmount;
    public Integer depositPercentage;
    public BigDecimal depositAmount;
    public BigDecimal balanceAmount;
    public String status;
    public String description;
    public LocalDate createdAt;
    public List<SplitPaymentItemDto> paymentItems;

    public SplitPaymentDto() {}

    public SplitPaymentDto(Long id, Long contractId, BigDecimal totalAmount, 
                          Integer depositPercentage, BigDecimal depositAmount, 
                          BigDecimal balanceAmount, String status, String description, 
                          LocalDate createdAt, List<SplitPaymentItemDto> paymentItems) {
        this.id = id;
        this.contractId = contractId;
        this.totalAmount = totalAmount;
        this.depositPercentage = depositPercentage;
        this.depositAmount = depositAmount;
        this.balanceAmount = balanceAmount;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.paymentItems = paymentItems;
    }
}

