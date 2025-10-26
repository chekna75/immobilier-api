package com.ditsolution.features.social.service;

import com.ditsolution.features.social.dto.*;
import com.ditsolution.features.social.entity.*;
import com.ditsolution.features.auth.entity.UserEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class SocialShareService {

    private static final Logger LOG = Logger.getLogger(SocialShareService.class);

    @Inject
    ObjectMapper objectMapper;

    /**
     * Enregistrer un partage social
     */
    @Transactional
    public SocialShareDto recordShare(CreateSocialShareRequest request, UserEntity user) {
        try {
            SocialShareEntity share = new SocialShareEntity();
            share.listingId = request.listingId;
            share.userId = user.getId();
            share.platform = SocialShareEntity.SocialPlatform.valueOf(request.platform.toUpperCase());
            share.shareType = SocialShareEntity.ShareType.valueOf(request.shareType.toUpperCase());
            share.sharedAt = OffsetDateTime.now();
            
            if (request.metadata != null) {
                share.metadata = objectMapper.writeValueAsString(request.metadata);
            }

            share.persist();

            LOG.info("Partage enregistré: " + share.id + " par " + user.getId() + " sur " + request.platform);

            return mapToDto(share, user);
        } catch (JsonProcessingException e) {
            LOG.error("Erreur lors de la sérialisation des métadonnées", e);
            throw new BadRequestException("Format des métadonnées invalide");
        } catch (Exception e) {
            LOG.error("Erreur lors de l'enregistrement du partage", e);
            throw new BadRequestException("Impossible d'enregistrer le partage: " + e.getMessage());
        }
    }

    /**
     * Récupérer les partages d'une annonce
     */
    public List<SocialShareDto> getListingShares(UUID listingId) {
        try {
            List<SocialShareEntity> shares = SocialShareEntity.findByListing(listingId);
            return shares.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des partages", e);
            throw new BadRequestException("Impossible de récupérer les partages");
        }
    }

    /**
     * Récupérer les partages d'un utilisateur
     */
    public List<SocialShareDto> getUserShares(UUID userId) {
        try {
            List<SocialShareEntity> shares = SocialShareEntity.findByUser(userId);
            return shares.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des partages utilisateur", e);
            throw new BadRequestException("Impossible de récupérer les partages utilisateur");
        }
    }

    /**
     * Obtenir les statistiques de partage
     */
    public SocialShareStatsDto getShareStats(UUID listingId) {
        try {
            List<SocialShareEntity> shares = SocialShareEntity.findByListing(listingId);
            
            if (shares.isEmpty()) {
                return new SocialShareStatsDto(0L, new HashMap<>(), new HashMap<>(), 
                    0L, 0L, new HashMap<>());
            }

            Long totalShares = (long) shares.size();
            
            // Partages par plateforme
            Map<String, Long> sharesByPlatform = shares.stream()
                .collect(Collectors.groupingBy(
                    share -> share.platform.getValue(),
                    Collectors.counting()
                ));

            // Partages par type
            Map<String, Long> sharesByType = shares.stream()
                .collect(Collectors.groupingBy(
                    share -> share.shareType.getValue(),
                    Collectors.counting()
                ));

            // Utilisateurs uniques
            Long uniqueUsers = shares.stream()
                .map(share -> share.userId)
                .distinct()
                .count();

            // Annonces uniques (toujours 1 pour une annonce spécifique)
            Long uniqueListings = 1L;

            // Partages récents (derniers 7 jours)
            OffsetDateTime weekAgo = OffsetDateTime.now().minusDays(7);
            Map<String, Long> recentShares = shares.stream()
                .filter(share -> share.sharedAt.isAfter(weekAgo))
                .collect(Collectors.groupingBy(
                    share -> share.sharedAt.toLocalDate().toString(),
                    Collectors.counting()
                ));

            return new SocialShareStatsDto(totalShares, sharesByPlatform, sharesByType, 
                uniqueUsers, uniqueListings, recentShares);
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des statistiques de partage", e);
            throw new BadRequestException("Impossible de calculer les statistiques de partage");
        }
    }

    /**
     * Obtenir les statistiques globales de partage
     */
    public SocialShareStatsDto getGlobalShareStats() {
        try {
            List<SocialShareEntity> allShares = SocialShareEntity.findAll().list();
            
            if (allShares.isEmpty()) {
                return new SocialShareStatsDto(0L, new HashMap<>(), new HashMap<>(), 
                    0L, 0L, new HashMap<>());
            }

            Long totalShares = (long) allShares.size();
            
            // Partages par plateforme
            Map<String, Long> sharesByPlatform = allShares.stream()
                .collect(Collectors.groupingBy(
                    share -> share.platform.getValue(),
                    Collectors.counting()
                ));

            // Partages par type
            Map<String, Long> sharesByType = allShares.stream()
                .collect(Collectors.groupingBy(
                    share -> share.shareType.getValue(),
                    Collectors.counting()
                ));

            // Utilisateurs uniques
            Long uniqueUsers = allShares.stream()
                .map(share -> share.userId)
                .distinct()
                .count();

            // Annonces uniques
            Long uniqueListings = allShares.stream()
                .map(share -> share.listingId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

            // Partages récents (derniers 7 jours)
            OffsetDateTime weekAgo = OffsetDateTime.now().minusDays(7);
            Map<String, Long> recentShares = allShares.stream()
                .filter(share -> share.sharedAt.isAfter(weekAgo))
                .collect(Collectors.groupingBy(
                    share -> share.sharedAt.toLocalDate().toString(),
                    Collectors.counting()
                ));

            return new SocialShareStatsDto(totalShares, sharesByPlatform, sharesByType, 
                uniqueUsers, uniqueListings, recentShares);
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des statistiques globales", e);
            throw new BadRequestException("Impossible de calculer les statistiques globales");
        }
    }

    /**
     * Mapper l'entité vers le DTO
     */
    private SocialShareDto mapToDto(SocialShareEntity share) {
        return mapToDto(share, null);
    }

    private SocialShareDto mapToDto(SocialShareEntity share, UserEntity user) {
        try {
            Map<String, Object> metadata = null;
            if (share.metadata != null) {
                metadata = objectMapper.readValue(share.metadata, 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            }

            SocialShareDto dto = new SocialShareDto();
            dto.id = UUID.fromString(share.id.toString());
            dto.listingId = share.listingId;
            dto.userId = share.userId;
            dto.userName = user != null ? (user.firstName + " " + user.lastName).trim() : "Utilisateur";
            dto.platform = share.platform.getValue();
            dto.shareType = share.shareType.getValue();
            dto.sharedAt = share.sharedAt;
            dto.metadata = metadata;

            return dto;
        } catch (Exception e) {
            LOG.error("Erreur lors du mapping du partage", e);
            throw new BadRequestException("Erreur lors du traitement du partage");
        }
    }
}
