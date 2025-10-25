package com.ditsolution.features.rental.repository;

import com.ditsolution.features.rental.entity.RentalContractEntity;
import com.ditsolution.features.auth.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RentalContractRepository implements PanacheRepository<RentalContractEntity> {

    public List<RentalContractEntity> findByOwner(UserEntity owner) {
        return find("owner", owner).list();
    }

    public List<RentalContractEntity> findByTenant(UserEntity tenant) {
        return find("tenant", tenant).list();
    }

    public List<RentalContractEntity> findByPropertyId(UUID propertyId) {
        return find("property.id", propertyId).list();
    }

    public List<RentalContractEntity> findActiveContracts() {
        return find("status", RentalContractEntity.ContractStatus.ACTIVE).list();
    }

    public List<RentalContractEntity> findActiveContractsByOwner(UserEntity owner) {
        return find("owner = ?1 and status = ?2", owner, RentalContractEntity.ContractStatus.ACTIVE).list();
    }

    public List<RentalContractEntity> findActiveContractsByTenant(UserEntity tenant) {
        return find("tenant = ?1 and status = ?2", tenant, RentalContractEntity.ContractStatus.ACTIVE).list();
    }

    public List<RentalContractEntity> findByOwnerId(Long ownerId) {
        return find("owner.id", ownerId).list();
    }
}
