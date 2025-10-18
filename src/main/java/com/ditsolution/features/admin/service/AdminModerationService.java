package com.ditsolution.features.admin.service;

import com.ditsolution.common.utils.HttpErrors;
import com.ditsolution.features.admin.dto.ModerateListingRequest;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.auth.service.AdminAuditService;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.enums.ListingStatus;
import com.ditsolution.features.listing.repository.ListingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class AdminModerationService {

    @Inject
    ListingRepository listingRepository;
    
    @Inject
    AdminAuditService auditService;

    @Transactional
    public ListingEntity moderateListing(UUID listingId, UserEntity admin, ModerateListingRequest request, String ip, String userAgent) {
        ListingEntity listing = listingRepository.findById(listingId);
        if (listing == null) {
            throw new NotFoundException("Annonce non trouvée");
        }

        String action = request.action().toUpperCase();
        
        switch (action) {
            case "REMOVE":
                return removeListing(listing, admin, request.reason(), ip, userAgent);
            case "RESTORE":
                return restoreListing(listing, admin, request.reason(), ip, userAgent);
            default:
                throw HttpErrors.badRequest("INVALID_ACTION", "Action invalide. Utilisez 'REMOVE' ou 'RESTORE'");
        }
    }

    private ListingEntity removeListing(ListingEntity listing, UserEntity admin, String reason, String ip, String userAgent) {
        if (listing.getStatus() == ListingStatus.REMOVED) {
            throw HttpErrors.badRequest("ALREADY_REMOVED", "L'annonce est déjà supprimée");
        }

        ListingStatus previousStatus = listing.getStatus();
        listing.setStatus(ListingStatus.REMOVED);
        listing.setUpdatedAt(java.time.OffsetDateTime.now().toInstant());
        
        // Log de l'action
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("previousStatus", previousStatus.name());
        details.put("listingTitle", listing.getTitle());
        details.put("ownerId", listing.getOwner().getId());
        
        try {
            auditService.log(
                admin.getId(),
                AdminAuditService.ACTION_LISTING_REMOVE,
                "LISTING",
                listing.getId(),
                new ObjectMapper().writeValueAsString(details),
                ip,
                userAgent
            );
        } catch (Exception e) {
            // Fallback si JSON échoue
            auditService.log(
                admin.getId(),
                AdminAuditService.ACTION_LISTING_REMOVE,
                "LISTING",
                listing.getId(),
                "reason: " + reason + ", previousStatus: " + previousStatus.name(),
                ip,
                userAgent
            );
        }

        return listing;
    }

    private ListingEntity restoreListing(ListingEntity listing, UserEntity admin, String reason, String ip, String userAgent) {
        if (listing.getStatus() != ListingStatus.REMOVED) {
            throw HttpErrors.badRequest("NOT_REMOVED", "L'annonce n'est pas supprimée");
        }

        // Restaurer en DRAFT par défaut (l'admin peut ensuite la publier)
        listing.setStatus(ListingStatus.DRAFT);
        listing.setUpdatedAt(java.time.OffsetDateTime.now().toInstant());
        
        // Log de l'action
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("newStatus", ListingStatus.DRAFT.name());
        details.put("listingTitle", listing.getTitle());
        details.put("ownerId", listing.getOwner().getId());
        
        try {
            auditService.log(
                admin.getId(),
                "LISTING_RESTORE",
                "LISTING",
                listing.getId(),
                new ObjectMapper().writeValueAsString(details),
                ip,
                userAgent
            );
        } catch (Exception e) {
            // Fallback si JSON échoue
            auditService.log(
                admin.getId(),
                "LISTING_RESTORE",
                "LISTING",
                listing.getId(),
                "reason: " + reason + ", newStatus: " + ListingStatus.DRAFT.name(),
                ip,
                userAgent
            );
        }

        return listing;
    }

    public java.util.List<ListingEntity> getModerationQueue() {
        // Retourner les annonces qui pourraient nécessiter une modération
        // Pour l'instant, on retourne les annonces récemment créées/modifiées
        return listingRepository.find("ORDER BY updatedAt DESC").page(0, 50).list();
    }
}
