package com.ditsolution.features.rental.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rent_payments")
@Data
@EqualsAndHashCode(callSuper = true)
public class RentPaymentEntity extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private RentalContractEntity contract;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_method")
    private String paymentMethod; // MOBILE_MONEY, CARD, BANK_TRANSFER

    @Column(name = "transaction_id")
    private String transactionId; // ID de la transaction CinetPay

    @Column(name = "cinetpay_transaction_id")
    private String cinetpayTransactionId;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "late_fee", precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "receipt_url")
    private String receiptUrl; // URL du PDF de reçu

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum PaymentStatus {
        PENDING, PAID, OVERDUE, CANCELLED, REFUNDED
    }
}
