package com.ditsolution.features.listing.mapper;

import com.ditsolution.features.listing.dto.ListingDto;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.entity.ListingPhotoEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ListingMapper {

    public ListingDto toDto(ListingEntity e) {
        List<String> urls = Optional.ofNullable(e.getPhotos())
                .orElse(List.of())
                .stream()
                .sorted(Comparator.comparing(
                        ListingPhotoEntity::getOrdering,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(ListingPhotoEntity::getUrl)
                .toList(); // Si tu es en Java 11: .collect(Collectors.toList())

        UUID ownerId = Optional.ofNullable(e.getOwner()).map(o -> o.getId()).orElse(null);

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
                
                // Géolocalisation
                e.getLatitude(),
                e.getLongitude(),
                
                // Champs enrichis
                e.getRooms(),
                e.getFloor(),
                e.getBuildingYear(),
                e.getEnergyClass(),
                e.getHasElevator(),
                e.getHasParking(),
                e.getHasBalcony(),
                e.getHasTerrace(),
                
                urls,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
