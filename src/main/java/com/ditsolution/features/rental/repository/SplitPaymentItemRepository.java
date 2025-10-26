package com.ditsolution.features.rental.repository;

import com.ditsolution.features.rental.entity.SplitPaymentItemEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SplitPaymentItemRepository implements PanacheRepository<SplitPaymentItemEntity> {

    public List<SplitPaymentItemEntity> findBySplitPaymentId(Long splitPaymentId) {
        return find("splitPayment.id", splitPaymentId).list();
    }

    public List<SplitPaymentItemEntity> findByStatus(SplitPaymentItemEntity.PaymentItemStatus status) {
        return find("status", status).list();
    }

    public Optional<SplitPaymentItemEntity> findByStripePaymentIntentId(String stripePaymentIntentId) {
        return find("stripePaymentIntentId", stripePaymentIntentId).firstResultOptional();
    }

    public List<SplitPaymentItemEntity> findOverduePayments() {
        return find("status = ?1 and dueDate < ?2", 
            SplitPaymentItemEntity.PaymentItemStatus.PENDING, 
            java.time.LocalDate.now()).list();
    }

    public List<SplitPaymentItemEntity> findByPaymentType(SplitPaymentItemEntity.PaymentType paymentType) {
        return find("paymentType", paymentType).list();
    }
}