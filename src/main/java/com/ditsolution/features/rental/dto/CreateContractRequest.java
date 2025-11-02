package com.ditsolution.features.rental.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateContractRequest {
    private UUID propertyId;
    private UUID tenantId;
    private BigDecimal monthlyRent;
    private BigDecimal deposit;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer paymentDueDay;
    private String notes;
}
