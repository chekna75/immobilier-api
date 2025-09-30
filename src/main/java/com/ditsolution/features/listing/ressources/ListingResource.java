package com.ditsolution.features.listing.ressources;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.dto.FiltersDto;
import com.ditsolution.features.listing.dto.ListingCreateDto;
import com.ditsolution.features.listing.dto.ListingUpdateDto;
import com.ditsolution.features.listing.dto.PageRequestDto;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.enums.ListingType;
import com.ditsolution.features.listing.mapper.ListingMapper;
import com.ditsolution.features.listing.services.ListingService;
import com.ditsolution.shared.dto.PagedResponse;

import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.UUID;



@Path("/listings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ListingResource {

    @Inject ListingService listingService;
    @Inject SecurityIdentity identity;
    @Inject ListingMapper mapper;

    // ---------------------------
    // 1. CREATE
    // ---------------------------
    @POST
    @Path("/create")
    @Authenticated
    @Transactional
    public Response create(ListingCreateDto dto) {
        var actor = currentUser();
        if (actor == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorDto("UNAUTHORIZED", "Utilisateur non authentifié")).build();
        }
        
        System.out.println("le role " + actor);
        
        // Vérification stricte des rôles autorisés
        if (actor.getRole() != UserEntity.Role.OWNER && actor.getRole() != UserEntity.Role.ADMIN) {
            System.out.println("Erreur de role: " + actor.getRole() + " - Seuls OWNER et ADMIN peuvent créer des annonces");
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorDto("FORBIDDEN", "Seuls les propriétaires (OWNER) et administrateurs (ADMIN) peuvent créer des annonces")).build();
        }
        
        ListingEntity l = listingService.createListing(actor, dto);
        return Response.status(Response.Status.CREATED).entity(mapper.toDto(l)).build();
    }

    // ---------------------------
    // 2. LIST (public, paginée avec filtres)
    // ---------------------------
    @GET
    public Response list(
        @QueryParam("city") String city,
        @QueryParam("district") String district,
        @QueryParam("type") String type,                // "RENT" | "SALE"
        @QueryParam("minPrice") BigDecimal minPrice,
        @QueryParam("maxPrice") BigDecimal maxPrice,
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("10") int size
    ) {
      // parse sûr de `type` -> ListingType
      ListingType listingType = null;
      if (type != null && !type.isBlank()) {
        try { listingType = ListingType.valueOf(type.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { /* type inconnu => pas de filtre */ }
      }
  
      var filters = new FiltersDto(city, district, listingType, minPrice, maxPrice);
      var pageReq = new PageRequestDto(page, size);
  
      var result = listingService.listListings(filters, pageReq); // <- ta signature
  
      var items = result.items().stream().map(mapper::toDto).toList();
      return Response.ok(new PagedResponse<>(items, result.total(), page, size)).build();
    }

    // ---------------------------
    // 3. GET by ID
    // ---------------------------
    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") UUID id) {
        var l = listingService.getListing(id);
        return Response.ok(mapper.toDto(l)).build();
    }

    // ---------------------------
    // 4. UPDATE
    // ---------------------------
    @PATCH
    @Path("/{id}")
    @RolesAllowed({"OWNER", "ADMIN"})
    @Transactional
    public Response update(@PathParam("id") UUID id, ListingUpdateDto dto) {
        var actor = currentUser();
        if(actor.getRole() == UserEntity.Role.TENANT){
            throw new ForbiddenException("Seuls OWNER ou ADMIN peuvent effectuer cette action");
        }
        var l = listingService.updateListing(id, actor, dto);
        return Response.ok(mapper.toDto(l)).build();
    }

    // ---------------------------
    // 5. GET USER LISTINGS
    // ---------------------------
    @GET
    @Path("/my")
    @Authenticated
    public Response getUserListings(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("10") int size
    ) {
        var actor = currentUser();
        if (actor == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorDto("UNAUTHORIZED", "Utilisateur non authentifié")).build();
        }
        
        var result = listingService.getUserListings(actor.getId(), page, size);
        var items = result.items().stream().map(mapper::toDto).toList();
        return Response.ok(new PagedResponse<>(items, result.total(), page, size)).build();
    }

    // ---------------------------
    // 6. DELETE (soft delete)
    // ---------------------------
    @DELETE
    @Path("/{id}")
    @RolesAllowed({"OWNER", "ADMIN"})
    @Transactional
    public Response delete(@PathParam("id") UUID id) {
        var actor = currentUser();
        if(actor.getRole() == UserEntity.Role.TENANT){
            throw new ForbiddenException("Seuls OWNER ou ADMIN peuvent effectuer cette action");
        }
        listingService.deleteListing(id, actor);
        return Response.noContent().build();
    }

    // ---------------------------
    // Helper
    // ---------------------------
    private UserEntity currentUser() {
        if (identity == null) {
            System.out.println("SecurityIdentity is null");
            return null;
        }
        
        var principal = identity.getPrincipal();
        if (principal == null) {
            System.out.println("Principal is null");
            return null;
        }
        
        try {
            UUID userId = UUID.fromString(principal.getName());
            System.out.println("User ID from token: " + userId);
            var user = (UserEntity) UserEntity.findById(userId);
            if (user == null) {
                System.out.println("User not found in database for ID: " + userId);
            } else {
                System.out.println("User found: " + user.email + " with role: " + user.role);
            }
            return user;
        } catch (Exception e) {
            System.out.println("Error parsing user ID from token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    // DTO pour les erreurs
    record ErrorDto(String error, String message) {}
}
