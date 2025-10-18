package com.ditsolution.features.listing.repository;

import java.util.List;
import java.util.UUID;

import com.ditsolution.features.listing.entity.ListingPhotoEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ListingPhotoRepository implements PanacheRepository<ListingPhotoEntity> {

    public List<ListingPhotoEntity> findByListingId(UUID listingId) {
        return find("listing.id = ?1 order by ordering asc", listingId).list();
    }
}
