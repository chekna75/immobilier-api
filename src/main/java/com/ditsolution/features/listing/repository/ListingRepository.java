package com.ditsolution.features.listing.repository;

import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.enums.ListingStatus;
import com.ditsolution.features.listing.enums.ListingType;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListingRepository implements PanacheRepository<ListingEntity> {

    /** Rechercher par id */
    public ListingEntity findById(UUID id) {
        return find("id", id).firstResult();
    }

    /**
     * Recherche filtrée par ville, type et fourchette de prix.
     */
    public List<ListingEntity> search(String city, ListingType type, BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        var query = new StringBuilder("status = :status");
        var params = new java.util.HashMap<String, Object>();
        params.put("status", ListingStatus.PUBLISHED);

        if (city != null && !city.isBlank()) {
            query.append(" AND LOWER(city) = :city");
            params.put("city", city.toLowerCase());
        }
        if (type != null) {
            query.append(" AND type = :type");
            params.put("type", type);
        }
        if (minPrice != null) {
            query.append(" AND price >= :minPrice");
            params.put("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.append(" AND price <= :maxPrice");
            params.put("maxPrice", maxPrice);
        }

        return find(query.toString(), params)
                .page(page, size)
                .list();
    }

    /**
     * Recherche par distance (rayon en km autour d'un point).
     * Utilise la formule de Haversine pour calculer la distance.
     */
    public List<ListingEntity> searchByDistance(BigDecimal centerLat, BigDecimal centerLng, double radiusKm, int page, int size) {
        String hql = """
            SELECT l FROM ListingEntity l 
            WHERE l.status = :status 
            AND l.latitude IS NOT NULL 
            AND l.longitude IS NOT NULL
            AND (
                6371 * acos(
                    cos(radians(:centerLat)) * 
                    cos(radians(l.latitude)) * 
                    cos(radians(l.longitude) - radians(:centerLng)) + 
                    sin(radians(:centerLat)) * 
                    sin(radians(l.latitude))
                )
            ) <= :radiusKm
            ORDER BY (
                6371 * acos(
                    cos(radians(:centerLat)) * 
                    cos(radians(l.latitude)) * 
                    cos(radians(l.longitude) - radians(:centerLng)) + 
                    sin(radians(:centerLat)) * 
                    sin(radians(l.latitude))
                )
            )
            """;

        Query query = getEntityManager().createQuery(hql, ListingEntity.class);
        query.setParameter("status", ListingStatus.PUBLISHED);
        query.setParameter("centerLat", centerLat);
        query.setParameter("centerLng", centerLng);
        query.setParameter("radiusKm", radiusKm);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        return query.getResultList();
    }

    /**
     * Recherche enrichie avec tous les filtres + géolocalisation.
     */
    public List<ListingEntity> searchEnriched(
            String city, 
            ListingType type, 
            BigDecimal minPrice, 
            BigDecimal maxPrice,
            Integer minRooms,
            Integer maxRooms,
            BigDecimal centerLat, 
            BigDecimal centerLng, 
            Double radiusKm,
            int page, 
            int size
    ) {
        var query = new StringBuilder("status = :status");
        var params = new java.util.HashMap<String, Object>();
        params.put("status", ListingStatus.PUBLISHED);

        if (city != null && !city.isBlank()) {
            query.append(" AND LOWER(city) = :city");
            params.put("city", city.toLowerCase());
        }
        if (type != null) {
            query.append(" AND type = :type");
            params.put("type", type);
        }
        if (minPrice != null) {
            query.append(" AND price >= :minPrice");
            params.put("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.append(" AND price <= :maxPrice");
            params.put("maxPrice", maxPrice);
        }
        if (minRooms != null) {
            query.append(" AND rooms >= :minRooms");
            params.put("minRooms", minRooms);
        }
        if (maxRooms != null) {
            query.append(" AND rooms <= :maxRooms");
            params.put("maxRooms", maxRooms);
        }

        // Si géolocalisation demandée, utiliser la recherche par distance
        if (centerLat != null && centerLng != null && radiusKm != null) {
            return searchByDistance(centerLat, centerLng, radiusKm, page, size);
        }

        return find(query.toString(), params)
                .page(page, size)
                .list();
    }
}
