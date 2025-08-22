package com.ditsolution.features.listing.mapper;

import com.ditsolution.features.listing.dto.ListingDto;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.entity.ListingPhotoEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListingMapper {

    public ListingDto toDto(ListingEntity e) {
        List<String> urls = e.getPhotos()
                .stream()
                .sorted((a,b) -> Integer.compare(a.getOrdering(), b.getOrdering()))
                .map(ListingPhotoEntity::getUrl)
                .toList();

        UUID ownerId = e.getOwner() != null ? e.getOwner().getId() : null;

        return new ListingDto(
                e.getId(),
                ownerId,
                e.getStatus(),
                e.getType(),
                e.getCity(),
                e.getDistrict(),
                e.getPrice(),
                e.getTitle(),
                e.getDescription(),
                urls,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
