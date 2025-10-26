package com.ditsolution.features.admin.service;

import com.ditsolution.features.admin.dto.AdminListingDto;
import com.ditsolution.features.admin.dto.AdminListingFilterDto;
import com.ditsolution.features.admin.dto.AdminListingUpdateDto;
import com.ditsolution.features.auth.entity.AdminLogEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AdminListingService {

    @Inject
    JsonWebToken jwt;

    @Inject
    EntityManager entityManager;

    @Transactional
    public List<AdminListingDto> getListings(AdminListingFilterDto filter) {
        StringBuilder query = new StringBuilder("SELECT l FROM ListingEntity l JOIN FETCH l.owner WHERE 1=1");
        
        if (filter.city() != null && !filter.city().isBlank()) {
            query.append(" AND LOWER(l.city) LIKE LOWER(:city)");
        }
        if (filter.district() != null && !filter.district().isBlank()) {
            query.append(" AND LOWER(l.district) LIKE LOWER(:district)");
        }
        if (filter.type() != null && !filter.type().isBlank()) {
            query.append(" AND l.type = :type");
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            query.append(" AND l.status = :status");
        }
        if (filter.ownerEmail() != null && !filter.ownerEmail().isBlank()) {
            query.append(" AND LOWER(l.owner.email) LIKE LOWER(:ownerEmail)");
        }
        if (filter.minPrice() != null) {
            query.append(" AND l.price >= :minPrice");
        }
        if (filter.maxPrice() != null) {
            query.append(" AND l.price <= :maxPrice");
        }
        
        query.append(" ORDER BY l.").append(filter.sortBy()).append(" ").append(filter.sortDirection());
        
        TypedQuery<ListingEntity> queryObj = entityManager.createQuery(query.toString(), ListingEntity.class);
        
        if (filter.city() != null && !filter.city().isBlank()) {
            queryObj.setParameter("city", "%" + filter.city() + "%");
        }
        if (filter.district() != null && !filter.district().isBlank()) {
            queryObj.setParameter("district", "%" + filter.district() + "%");
        }
        if (filter.type() != null && !filter.type().isBlank()) {
            queryObj.setParameter("type", filter.type());
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            queryObj.setParameter("status", filter.status());
        }
        if (filter.ownerEmail() != null && !filter.ownerEmail().isBlank()) {
            queryObj.setParameter("ownerEmail", "%" + filter.ownerEmail() + "%");
        }
        if (filter.minPrice() != null) {
            queryObj.setParameter("minPrice", filter.minPrice());
        }
        if (filter.maxPrice() != null) {
            queryObj.setParameter("maxPrice", filter.maxPrice());
        }
        
        List<ListingEntity> listings = queryObj
            .setFirstResult(filter.page() * filter.size())
            .setMaxResults(filter.size())
            .getResultList();
        
        return listings.stream().map(this::mapToAdminListingDto).collect(Collectors.toList());
    }
    
    @Transactional
    public AdminListingDto getListingById(UUID listingId) {
        ListingEntity listing = entityManager.find(ListingEntity.class, listingId);
        if (listing == null) {
            throw new RuntimeException("Annonce non trouvée");
        }
        return mapToAdminListingDto(listing);
    }
    
    @Transactional
    public void updateListings(AdminListingUpdateDto updateDto) {
        for (UUID listingId : updateDto.listingIds()) {
            ListingEntity listing = entityManager.find(ListingEntity.class, listingId);
            if (listing != null) {
                // Log de l'action admin
                logAdminAction("LISTING_UPDATE", listingId.toString(), updateDto.reason());
                
                if (updateDto.status() != null && !updateDto.status().isBlank()) {
                    // Utiliser l'enum ListingStatus
                    listing.setStatus(com.ditsolution.features.listing.enums.ListingStatus.valueOf(updateDto.status()));
                }
                
                entityManager.merge(listing);
            }
        }
    }
    
    private AdminListingDto mapToAdminListingDto(ListingEntity listing) {
        List<String> photoUrls = new ArrayList<>();
        if (listing.getPhotos() != null) {
            photoUrls = listing.getPhotos().stream()
                .sorted((p1, p2) -> Integer.compare(p1.getOrdering(), p2.getOrdering()))
                .map(p -> p.getUrl())
                .collect(Collectors.toList());
        }
        
        return new AdminListingDto(
            listing.getId(),
            listing.getTitle(),
            listing.getDescription(),
            listing.getCity(),
            listing.getDistrict(),
            listing.getPrice(),
            listing.getType() != null ? listing.getType().name() : null,
            listing.getStatus() != null ? listing.getStatus().name() : null,
            false, // Système premium non implémenté - toutes les annonces sont gratuites
            listing.getCreatedAt() != null ? listing.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null,
            listing.getUpdatedAt() != null ? listing.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : null,
            listing.getOwner().getId(),
            listing.getOwner().getEmail(),
            listing.getOwner().getFirstName(),
            listing.getOwner().getLastName(),
            photoUrls,
            listing.getRooms(),
            listing.getFloor(),
            listing.getBuildingYear(),
            listing.getEnergyClass(),
            listing.getHasElevator(),
            listing.getHasParking(),
            listing.getHasBalcony(),
            listing.getHasTerrace(),
            listing.getLatitude(),
            listing.getLongitude()
        );
    }
    
    private void logAdminAction(String action, String targetId, String reason) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        
        AdminLogEntity log = new AdminLogEntity();
        log.adminId = adminId;
        log.action = action;
        log.targetType = "LISTING";
        log.targetId = UUID.fromString(targetId);
        log.details = createDetailsJson(reason);
        log.persist();
    }
    
    private com.fasterxml.jackson.databind.JsonNode createDetailsJson(String reason) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.createObjectNode().put("reason", reason);
        } catch (Exception e) {
            return null;
        }
    }
}
