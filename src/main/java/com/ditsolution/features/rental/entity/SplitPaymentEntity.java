package com.ditsolution.features.rental.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "split_payments")
@Data
@EqualsAndHashCode(callSuper = true)
public class SplitPaymentEntity extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private RentalContractEntity contract;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "deposit_percentage", nullable = false)
    private Integer depositPercentage = 30;

    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "balance_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SplitPaymentStatus status = SplitPaymentStatus.PENDING;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "splitPayment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SplitPaymentItemEntity> paymentItems;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum SplitPaymentStatus {
        PENDING,    // En attente de paiement de l'acompte
        DEPOSIT_PAID, // Acompte payé, en attente du solde
        COMPLETED,  // Tous les paiements effectués
        CANCELLED,  // Annulé
        FAILED      // Échec
    }
}