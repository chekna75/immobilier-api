package com.ditsolution.features.rental.repository;

import com.ditsolution.features.rental.entity.SplitPaymentEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SplitPaymentRepository implements PanacheRepository<SplitPaymentEntity> {

    public List<SplitPaymentEntity> findByContractId(Long contractId) {
        return find("contract.id", contractId).list();
    }

    public List<SplitPaymentEntity> findByStatus(SplitPaymentEntity.SplitPaymentStatus status) {
        return find("status", status).list();
    }

    public Optional<SplitPaymentEntity> findByContractIdAndStatus(Long contractId, SplitPaymentEntity.SplitPaymentStatus status) {
        return find("contract.id = ?1 and status = ?2", contractId, status).firstResultOptional();
    }

    public List<SplitPaymentEntity> findActiveSplitPayments() {
        return find("status in ?1", List.of(
            SplitPaymentEntity.SplitPaymentStatus.PENDING,
            SplitPaymentEntity.SplitPaymentStatus.DEPOSIT_PAID
        )).list();
    }
}