package com.ditsolution.features.listing.repository;

import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.enums.ListingType;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListingRepository implements PanacheRepository<ListingEntity> {

    /** Rechercher par id */
    public ListingEntity findById(UUID id) {
        return findById(id);
    }

    /**
     * Recherche filtrée par ville, type et fourchette de prix.
     */
    public List<ListingEntity> search(String city, ListingType type, BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        var query = new StringBuilder("1=1");
        var params = new java.util.HashMap<String, Object>();

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
}
