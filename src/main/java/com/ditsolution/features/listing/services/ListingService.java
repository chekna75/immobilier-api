package com.ditsolution.features.listing.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;

import com.ditsolution.common.services.BaseService;
import com.ditsolution.common.utils.HttpErrors;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.dto.FiltersDto;
import com.ditsolution.features.listing.dto.ListingCreateDto;
import com.ditsolution.features.listing.dto.ListingUpdateDto;
import com.ditsolution.features.listing.dto.PageRequestDto;
import com.ditsolution.features.listing.dto.PagedDto;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.entity.ListingPhotoEntity;
import com.ditsolution.features.listing.enums.ListingStatus;
import com.ditsolution.features.listing.repository.ListingPhotoRepository;
import com.ditsolution.features.listing.repository.ListingRepository;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;


public class ListingService extends BaseService{

    private static final int MAX_PHOTOS = 5;

    @Inject ListingRepository listingRepo;
    @Inject ListingPhotoRepository photoRepo;

    // =========================
    // Méthodes métier
    // =========================

    @Transactional
    public ListingEntity createListing(UserEntity owner, ListingCreateDto dto) {
        // Autorisation : seuls OWNER/ADMIN peuvent créer (ajuste si tu veux autoriser TENANT)
        requireOwnerOrAdmin(owner);

        validateCreate(dto);

        var now = OffsetDateTime.now();

        var listing = new ListingEntity();
        listing.setOwner(owner);
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setType(dto.type());
        listing.setCity(trim(dto.city()));
        listing.setDistrict(trim(dto.district()));
        listing.setPrice(dto.price());
        listing.setTitle(trim(dto.title()));
        listing.setDescription(dto.description());
        listing.setCreatedAt(now.toInstant());
        listing.setUpdatedAt(now.toInstant());

        listingRepo.persist(listing);

        // Photos (≤ 5)
        var photos = safeList(dto.photos());
        if (photos.size() > MAX_PHOTOS) {
            throw HttpErrors.badRequest("PHOTOS_LIMIT", "Maximum 5 photos");
        }
        int ordering = 0;
        for (String url : photos) {
            if (isBlank(url)) continue;
            var p = new ListingPhotoEntity();
            p.setListing(listing);
            p.setUrl(url.trim());
            p.setOrdering(ordering++);
            photoRepo.persist(p);
        }

        return listing;
    }

    public PagedDto<ListingEntity> listListings(FiltersDto f, PageRequestDto page) {
        // Filtre uniquement les ACTIVE (soft delete caché)
        var where = new StringBuilder("status = :active");
        var params = new HashMap<String, Object>();
        params.put("active", ListingStatus.ACTIVE);

        if (!isBlank(f.city())) {
            where.append(" AND LOWER(city) = :city");
            params.put("city", f.city().toLowerCase());
        }
        if (!isBlank(f.district())) {
            where.append(" AND LOWER(district) = :district");
            params.put("district", f.district().toLowerCase());
        }
        if (f.type() != null) {
            where.append(" AND type = :type");
            params.put("type", f.type());
        }
        if (f.minPrice() != null) {
            where.append(" AND price >= :minPrice");
            params.put("minPrice", f.minPrice());
        }
        if (f.maxPrice() != null) {
            where.append(" AND price <= :maxPrice");
            params.put("maxPrice", f.maxPrice());
        }

        var q = listingRepo.find(where.toString(), params).page(page.page(), page.size());
        var items = q.list();
        long total = listingRepo.count(where.toString(), params);

        return new PagedDto<>(items, total, page.page(), page.size());
    }

    public ListingEntity getListing(UUID id) {
        var l = listingRepo.findById(id);
        if (l == null || l.getStatus() == ListingStatus.REMOVED) {
            throw new NotFoundException("Listing not found");
        }
        // si tes photos sont LAZY et tu veux les charger ici, tu peux faire :
        // l.photos = photoRepo.findByListingId(l.id); (selon ton modèle)
        return l;
    }

    @Transactional
    public ListingEntity updateListing(UUID id, UserEntity actor, ListingUpdateDto dto) {
        var l = listingRepo.findById(id);
        if (l == null) throw new NotFoundException("Listing not found");
        ensureOwnerOrAdmin(l, actor);

        if (dto.title().isPresent() && !isBlank(dto.title().get())) {
            l.setTitle(dto.title().get().trim());
        }
        
        if (dto.description().isPresent()) {
            l.setDescription(dto.description().get());
        }
        
        if (dto.city().isPresent() && !isBlank(dto.city().get())) {
            l.setCity(dto.city().get().trim());
        }
        
        if (dto.district().isPresent() && !isBlank(dto.district().get())) {
            l.setDistrict(dto.district().get().trim());
        }
        
        dto.type().ifPresent(l::setType);
        dto.price().ifPresent(l::setPrice);

        // Remplacement complet des photos si fourni
        if (dto.photos().isPresent()) {
            var photos = safeList(dto.photos().get());
            if (photos.size() > MAX_PHOTOS) throw HttpErrors.badRequest("PHOTOS_LIMIT", "Maximum 5 photos");

            photoRepo.delete("listing", l);
            int ordering = 0;
            for (String url : photos) {
                if (isBlank(url)) continue;
                var p = new ListingPhotoEntity();
                p.setListing(l);
                p.setUrl(url.trim());
                p.setOrdering(ordering++);
                photoRepo.persist(p);
            }
        }

        l.setUpdatedAt(OffsetDateTime.now().toInstant());
        return l;
    }

    @Transactional
    public void deleteListing(UUID id, UserEntity actor) {
        var l = listingRepo.findById(id);
        if (l == null) throw new NotFoundException("Listing not found");
        ensureOwnerOrAdmin(l, actor);
        l.setStatus(ListingStatus.REMOVED); // soft delete
        l.setUpdatedAt(OffsetDateTime.now().toInstant());
    }

    private void validateCreate(ListingCreateDto dto) {
        if (dto == null) throw badRequest("VALIDATION_ERROR", "payload requis");
        if (dto.title() == null || dto.title().isBlank())
            throw badRequest("VALIDATION_ERROR", "title est requis");
        if (dto.city() == null || dto.city().isBlank())
            throw badRequest("VALIDATION_ERROR", "city est requis");
        if (dto.type() == null)
            throw badRequest("VALIDATION_ERROR", "type est requis");
        if (dto.price() == null || dto.price().compareTo(BigDecimal.ZERO) < 0)
            throw badRequest("VALIDATION_ERROR", "price doit être ≥ 0");
        if (dto.photos() != null && dto.photos().size() > 5)
            throw badRequest("PHOTOS_LIMIT", "Maximum 5 photos");
    }

     // ------- règles d’accès -------

     private void ensureOwnerOrAdmin(ListingEntity l, UserEntity actor) {
        if (actor == null) throw unauthorized("UNAUTHORIZED", "Utilisateur requis");
        if (actor.role == UserEntity.Role.ADMIN) return;
        if (l.getOwner() != null && l.getOwner().getId() != null && l.getOwner().getId().equals(actor.getId())) return;
        throw forbidden("FORBIDDEN", "Seul le propriétaire ou un admin peut modifier/supprimer");
    }

    private void requireOwnerOrAdmin(UserEntity actor) {
        if (actor == null) throw unauthorized("UNAUTHORIZED", "Utilisateur requis");
        if (actor.role == UserEntity.Role.ADMIN || actor.role == UserEntity.Role.OWNER) return;
        throw forbidden("FORBIDDEN", "Seuls OWNER ou ADMIN peuvent créer une annonce");
    }
    
}
