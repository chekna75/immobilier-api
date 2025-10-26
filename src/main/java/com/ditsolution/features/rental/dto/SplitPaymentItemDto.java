package com.ditsolution.features.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SplitPaymentItemDto {
    public Long id;
    public String paymentType;
    public BigDecimal amount;
    public LocalDate dueDate;
    public LocalDate paidDate;
    public String status;
    public String paymentMethod;
    public String transactionId;
    public String receiptUrl;
    public String notes;

    public SplitPaymentItemDto() {}

    public SplitPaymentItemDto(Long id, String paymentType, BigDecimal amount, 
                              LocalDate dueDate, LocalDate paidDate, String status, 
                              String paymentMethod, String transactionId, String receiptUrl, String notes) {
        this.id = id;
        this.paymentType = paymentType;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paidDate = paidDate;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.receiptUrl = receiptUrl;
        this.notes = notes;
    }
}
