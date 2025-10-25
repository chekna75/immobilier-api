package com.ditsolution.features.rental.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentTransactionEntity extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rent_payment_id", nullable = false)
    private RentPaymentEntity rentPayment;

    @Column(name = "stripe_payment_intent_id", unique = true, nullable = false)
    private String stripePaymentIntentId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "payment_method_id")
    private String paymentMethodId;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "success_url")
    private String successUrl;

    @Column(name = "cancel_url")
    private String cancelUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON des métadonnées CinetPay

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

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

    public enum TransactionStatus {
        PENDING, SUCCESS, FAILED, CANCELLED, EXPIRED
    }

    public enum PaymentMethod {
        CARD, SEPA_DEBIT, BANCONTACT, IDEAL, SOFORT
    }
}
