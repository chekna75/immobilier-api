package com.ditsolution.features.listing.service;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.dto.FavoriteDto;
import com.ditsolution.features.listing.entity.FavoriteEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.repository.ListingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class FavoriteService {

    @Inject
    ListingRepository listingRepository;

    /**
     * Récupère tous les favoris d'un utilisateur
     */
    public List<FavoriteDto> getUserFavorites(UUID userId) {
        List<FavoriteEntity> favorites = FavoriteEntity.find(
                "SELECT f FROM FavoriteEntity f JOIN FETCH f.user JOIN FETCH f.listing LEFT JOIN FETCH f.listing.photos WHERE f.user.id = ?1", 
                userId)
                .list();
        
        return favorites.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Ajoute un bien aux favoris d'un utilisateur
     */
    @Transactional
    public FavoriteDto addFavorite(UUID userId, UUID listingId) {
        // Vérifier que le bien existe
        ListingEntity listing = listingRepository.findById(listingId);
        if (listing == null) {
            throw new NotFoundException("Bien non trouvé");
        }

        // Vérifier si le favori existe déjà
        FavoriteEntity existingFavorite = FavoriteEntity.find("user.id = ?1 and listing.id = ?2", userId, listingId)
                .firstResult();
        
        if (existingFavorite != null) {
            return mapToDto(existingFavorite);
        }

        // Créer le nouveau favori
        FavoriteEntity favorite = new FavoriteEntity();
        favorite.setUser(UserEntity.findById(userId));
        favorite.setListing(listing);
        favorite.persist();

        // Recharger avec les relations pour le mapping
        FavoriteEntity savedFavorite = FavoriteEntity.find(
                "SELECT f FROM FavoriteEntity f JOIN FETCH f.user JOIN FETCH f.listing LEFT JOIN FETCH f.listing.photos WHERE f.id = ?1", 
                favorite.id)
                .firstResult();

        return mapToDto(savedFavorite);
    }

    /**
     * Supprime un bien des favoris d'un utilisateur
     */
    @Transactional
    public void removeFavorite(UUID userId, UUID listingId) {
        FavoriteEntity favorite = FavoriteEntity.find("user.id = ?1 and listing.id = ?2", userId, listingId)
                .firstResult();
        
        if (favorite != null) {
            favorite.delete();
        }
    }

    /**
     * Vérifie si un bien est dans les favoris d'un utilisateur
     */
    public boolean isFavorite(UUID userId, UUID listingId) {
        return FavoriteEntity.find("user.id = ?1 and listing.id = ?2", userId, listingId)
                .count() > 0;
    }

    /**
     * Bascule l'état favori d'un bien (ajoute s'il n'existe pas, supprime s'il existe)
     */
    @Transactional
    public boolean toggleFavorite(UUID userId, UUID listingId) {
        try {
            boolean isCurrentlyFavorite = isFavorite(userId, listingId);
            
            if (isCurrentlyFavorite) {
                removeFavorite(userId, listingId);
                return false;
            } else {
                // Version simplifiée pour le debug
                FavoriteEntity favorite = new FavoriteEntity();
                favorite.setUser(UserEntity.findById(userId));
                favorite.setListing(listingRepository.findById(listingId));
                favorite.persist();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du basculement des favoris: " + e.getMessage(), e);
        }
    }

    /**
     * Mappe une entité FavoriteEntity vers un DTO
     */
    private FavoriteDto mapToDto(FavoriteEntity favorite) {
        FavoriteDto dto = new FavoriteDto();
        dto.setId(favorite.id);
        dto.setUserId(favorite.getUser().getId());
        dto.setListingId(favorite.getListing().getId());
        dto.setCreatedAt(favorite.getCreatedAt());
        
        // Informations du bien
        dto.setListingTitle(favorite.getListing().getTitle());
        dto.setListingCity(favorite.getListing().getCity());
        dto.setListingDistrict(favorite.getListing().getDistrict());
        dto.setListingPrice(favorite.getListing().getPrice() != null ? favorite.getListing().getPrice().toString() : "0");
        
        // Récupérer la première photo comme miniature
        if (favorite.getListing().getPhotos() != null && !favorite.getListing().getPhotos().isEmpty()) {
            dto.setListingThumbnailUrl(favorite.getListing().getPhotos().get(0).getUrl());
        }
        
        return dto;
    }
}
