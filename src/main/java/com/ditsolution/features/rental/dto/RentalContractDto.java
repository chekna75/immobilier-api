package com.ditsolution.features.rental.dto;

import com.ditsolution.features.auth.dto.UserDto;
import com.ditsolution.features.listing.dto.ListingDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class RentalContractDto {
    private Long id;
    private String contractNumber;
    private ListingDto property;
    private UserDto owner;
    private UserDto tenant;
    private BigDecimal monthlyRent;
    private BigDecimal deposit;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer paymentDueDay;
    private String status;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<RentPaymentDto> payments;
}
