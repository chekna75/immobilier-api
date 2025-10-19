package com.ditsolution.features.rental.repository;

import com.ditsolution.features.rental.entity.RentPaymentEntity;
import com.ditsolution.features.rental.entity.RentalContractEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class RentPaymentRepository implements PanacheRepository<RentPaymentEntity> {

    public List<RentPaymentEntity> findByContract(RentalContractEntity contract) {
        return find("contract", contract).list();
    }

    public List<RentPaymentEntity> findPendingPayments() {
        return find("status", RentPaymentEntity.PaymentStatus.PENDING).list();
    }

    public List<RentPaymentEntity> findOverduePayments() {
        return find("status = ?1 and dueDate < ?2", 
                   RentPaymentEntity.PaymentStatus.PENDING, LocalDate.now()).list();
    }

    public List<RentPaymentEntity> findPendingPaymentsByContract(RentalContractEntity contract) {
        return find("contract = ?1 and status = ?2", 
                   contract, RentPaymentEntity.PaymentStatus.PENDING).list();
    }

    public List<RentPaymentEntity> findOverduePaymentsByContract(RentalContractEntity contract) {
        return find("contract = ?1 and status = ?2 and dueDate < ?3", 
                   contract, RentPaymentEntity.PaymentStatus.PENDING, LocalDate.now()).list();
    }

    public List<RentPaymentEntity> findPaidPaymentsByContract(RentalContractEntity contract) {
        return find("contract = ?1 and status = ?2", 
                   contract, RentPaymentEntity.PaymentStatus.PAID).list();
    }

    public List<RentPaymentEntity> findByOwnerAndPeriod(Long ownerId, LocalDate startDate, LocalDate endDate) {
        return find("contract.owner.id = ?1 AND dueDate >= ?2 AND dueDate <= ?3", 
                   ownerId, startDate, endDate).list();
    }
}
