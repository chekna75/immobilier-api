package com.ditsolution.features.listing.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.enums.ListingStatus;
import com.ditsolution.features.listing.enums.ListingType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "listings")
public class ListingEntity {
    @Id @GeneratedValue private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserEntity owner;

    @Enumerated(EnumType.STRING) 
    private ListingStatus status = ListingStatus.DRAFT;
    
    @Enumerated(EnumType.STRING) 
    private ListingType type;
    private String city;
    private String district;
    private BigDecimal price;
    private String title;
    private String description;
    
    // Géolocalisation
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    
    // Champs enrichis
    private Integer rooms;
    private Integer floor;
    private Integer buildingYear;
    private String energyClass;
    private Boolean hasElevator = false;
    private Boolean hasParking = false;
    private Boolean hasBalcony = false;
    private Boolean hasTerrace = false;
    
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListingPhotoEntity> photos = new ArrayList<>();
}
