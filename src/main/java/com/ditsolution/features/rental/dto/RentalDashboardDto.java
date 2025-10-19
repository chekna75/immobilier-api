package com.ditsolution.features.rental.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class RentalDashboardDto {
    private BigDecimal totalMonthlyIncome;
    private BigDecimal totalCollected;
    private BigDecimal totalPending;
    private BigDecimal totalOverdue;
    private Integer activeContracts;
    private Integer pendingPayments;
    private Integer overduePaymentsCount;
    private List<MonthlyIncomeDto> monthlyIncome;
    private List<RentPaymentDto> recentPayments;
    private List<RentPaymentDto> overduePaymentsList;
}

@Data
class MonthlyIncomeDto {
    private String month;
    private BigDecimal amount;
    private Integer paymentCount;
}
