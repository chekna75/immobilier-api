package com.ditsolution.features.rental.dto;

import lombok.Data;

@Data
public class PaymentResponse {
    private String paymentUrl;
    private String transactionId;
    private String status;
    private String message;
}
