package com.ditsolution.features.rental.dto;

import lombok.Data;

@Data
public class InitiatePaymentRequest {
    private Long rentPaymentId;
    private String paymentMethod; // MOBILE_MONEY, CARD, BANK_TRANSFER
    private String phoneNumber;
    private String operator; // ORANGE, MTN, MOOV
    private String clientIp;
    private String userAgent;
}
