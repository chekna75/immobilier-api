package com.ditsolution.features.rental.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class RentPaymentDto {
    private Long id;
    private Long contractId;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private String status;
    private String paymentMethod;
    private String transactionId;
    private String cinetpayTransactionId;
    private String paymentReference;
    private BigDecimal lateFee;
    private String notes;
    private String receiptUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
