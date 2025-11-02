package com.ditsolution.features.rental.repository;

import com.ditsolution.features.rental.entity.PaymentTransactionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class PaymentTransactionRepository implements PanacheRepository<PaymentTransactionEntity> {

    public Optional<PaymentTransactionEntity> findByStripePaymentIntentId(String stripePaymentIntentId) {
        return find("stripePaymentIntentId", stripePaymentIntentId).firstResultOptional();
    }

    public Optional<PaymentTransactionEntity> findByTransactionId(String transactionId) {
        return find("transactionId", transactionId).firstResultOptional();
    }
}
