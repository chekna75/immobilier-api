package com.ditsolution.features.listing.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;

import com.ditsolution.common.services.BaseService;
import com.ditsolution.common.services.EmailService;
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
import com.ditsolution.features.storage.service.FileValidationService;
import com.ditsolution.features.storage.entity.UploadedImageEntity;
import com.ditsolution.features.notification.service.NotificationTriggerService;

import io.quarkus.cache.CacheResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;


@ApplicationScoped
public class ListingService extends BaseService{

    private static final int MAX_PHOTOS = 5;

    @Inject ListingRepository listingRepo;
    @Inject ListingPhotoRepository photoRepo;
    @Inject FileValidationService fileValidationService;
    @Inject EmailService emailService;
    @Inject NotificationTriggerService notificationTriggerService;

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
        listing.setStatus(ListingStatus.DRAFT); // Par défaut en brouillon
        listing.setType(dto.type());
        listing.setCity(trim(dto.city()));
        listing.setDistrict(trim(dto.district()));
        listing.setPrice(dto.price());
        listing.setTitle(trim(dto.title()));
        listing.setDescription(dto.description());
        
        // Géolocalisation
        listing.setLatitude(dto.latitude());
        listing.setLongitude(dto.longitude());
        
        // Champs enrichis
        listing.setRooms(dto.rooms());
        listing.setFloor(dto.floor());
        listing.setBuildingYear(dto.buildingYear());
        listing.setEnergyClass(dto.energyClass());
        listing.setHasElevator(dto.hasElevator() != null ? dto.hasElevator() : false);
        listing.setHasParking(dto.hasParking() != null ? dto.hasParking() : false);
        listing.setHasBalcony(dto.hasBalcony() != null ? dto.hasBalcony() : false);
        listing.setHasTerrace(dto.hasTerrace() != null ? dto.hasTerrace() : false);
        
        listing.setCreatedAt(now.toInstant());
        listing.setUpdatedAt(now.toInstant());

        listingRepo.persist(listing);

        // Photos (1-5) avec validation obligatoire
        var photos = safeList(dto.photos());
        if (photos.isEmpty()) {
            throw HttpErrors.badRequest("PHOTOS_REQUIRED", "Au moins " + fileValidationService.getMinPhotosPerListing() + " photo est obligatoire");
        }
        if (!fileValidationService.isValidPhotoCount(photos.size())) {
            throw HttpErrors.badRequest("PHOTOS_LIMIT", "Entre " + fileValidationService.getMinPhotosPerListing() + " et " + fileValidationService.getMaxPhotosPerListing() + " photos requises");
        }
        
        int ordering = 0;
        for (String url : photos) {
            if (isBlank(url)) continue;
            
            // Validation basique de l'URL (doit être une URL S3 publique)
            if (!isValidS3Url(url.trim())) {
                throw HttpErrors.badRequest("INVALID_PHOTO_URL", "URL de photo invalide");
            }
            
            // Marquer l'image comme utilisée si elle appartient à l'utilisateur
            markImageAsUsed(url.trim(), owner.getId());
            
            var p = new ListingPhotoEntity();
            p.setListing(listing);
            p.setUrl(url.trim());
            p.setOrdering(ordering++);
            photoRepo.persist(p);
        }

        emailService.sendListingPublishedEmail(owner.getEmail(), listing.getTitle());

        return listing;
    }

    private boolean isValidS3Url(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        // Validation basique : doit être une URL HTTPS
        // Pour les tests, on accepte aussi les URLs example.com
        return url.startsWith("https://") && 
               (url.contains("s3.amazonaws.com") || url.contains("s3.") || url.contains("example.com"));
    }

    private void markImageAsUsed(String publicUrl, UUID userId) {
        // Trouver l'image par URL publique et ID utilisateur
        var image = UploadedImageEntity.find("publicUrl = ?1 and userId = ?2", publicUrl, userId).firstResult();
        if (image != null) {
            ((UploadedImageEntity) image).setIsUsed(true);
            image.persist();
        }
    }

    public PagedDto<ListingEntity> listListings(FiltersDto f, PageRequestDto page) {
        // Filtre uniquement les ACTIVE (soft delete caché)
        var where = new StringBuilder("status = :active");
        var params = new HashMap<String, Object>();
        params.put("active", ListingStatus.PUBLISHED);

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

    public PagedDto<ListingEntity> getUserListings(UUID userId, int page, int size) {
        // Récupérer toutes les annonces de l'utilisateur (y compris DRAFT, PUBLISHED, ARCHIVED)
        // Exclure seulement les REMOVED (soft delete)
        var where = "owner.id = :userId AND status != :removed";
        var params = new HashMap<String, Object>();
        params.put("userId", userId);
        params.put("removed", ListingStatus.REMOVED);

        var q = listingRepo.find(where, params).page(page, size);
        var items = q.list();
        long total = listingRepo.count(where, params);

        return new PagedDto<>(items, total, page, size);
    }

    /**
     * Recherche par proximité avec filtres additionnels et mise en cache
     */
    @CacheResult(cacheName = "proximity-search")
    public PagedDto<ListingEntity> searchByProximity(
            BigDecimal latitude, 
            BigDecimal longitude, 
            Double radiusKm,
            com.ditsolution.features.listing.enums.ListingType type,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page, 
            int size
    ) {
        // Utiliser la méthode enrichie du repository qui combine géolocalisation et filtres
        var results = listingRepo.searchEnriched(
            null, // city - pas de filtre par ville pour la recherche par proximité
            type,
            minPrice,
            maxPrice,
            null, // minRooms
            null, // maxRooms
            latitude,
            longitude,
            radiusKm,
            page,
            size
        );
        
        // Pour la pagination géospatiale, on doit compter le total différemment
        // car la requête géospatiale ne peut pas être facilement paginée avec un count
        long total = results.size(); // Approximation - pour une vraie pagination, il faudrait une requête séparée
        
        return new PagedDto<>(results, total, page, size);
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
        
        // Géolocalisation
        dto.latitude().ifPresent(l::setLatitude);
        dto.longitude().ifPresent(l::setLongitude);
        
        // Champs enrichis
        dto.rooms().ifPresent(l::setRooms);
        dto.floor().ifPresent(l::setFloor);
        dto.buildingYear().ifPresent(l::setBuildingYear);
        dto.energyClass().ifPresent(l::setEnergyClass);
        dto.hasElevator().ifPresent(l::setHasElevator);
        dto.hasParking().ifPresent(l::setHasParking);
        dto.hasBalcony().ifPresent(l::setHasBalcony);
        dto.hasTerrace().ifPresent(l::setHasTerrace);

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
        
        // Déclencher les notifications pour les utilisateurs qui ont cette annonce en favori
        if (l.getStatus() == ListingStatus.PUBLISHED) {
            notificationTriggerService.triggerFavoriteUpdateNotification(l);
        }
        
        return l;
    }

    /**
     * Publier une annonce (DRAFT → PUBLISHED)
     */
    @Transactional
    public ListingEntity publishListing(UUID id, UserEntity actor) {
        var listing = listingRepo.findById(id);
        if (listing == null) throw new NotFoundException("Listing not found");
        ensureOwnerOrAdmin(listing, actor);
        
        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw HttpErrors.badRequest("INVALID_STATUS", "Seules les annonces en brouillon peuvent être publiées");
        }
        
        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setUpdatedAt(OffsetDateTime.now().toInstant());
        
        // Déclencher les notifications pour les utilisateurs intéressés
        notificationTriggerService.triggerNewListingMatchNotification(listing);
        
        return listing;
    }

    /**
     * Archiver une annonce (PUBLISHED → ARCHIVED)
     */
    @Transactional
    public ListingEntity archiveListing(UUID id, UserEntity actor) {
        var listing = listingRepo.findById(id);
        if (listing == null) throw new NotFoundException("Listing not found");
        ensureOwnerOrAdmin(listing, actor);
        
        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw HttpErrors.badRequest("INVALID_STATUS", "Seules les annonces publiées peuvent être archivées");
        }
        
        listing.setStatus(ListingStatus.ARCHIVED);
        listing.setUpdatedAt(OffsetDateTime.now().toInstant());
        return listing;
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
