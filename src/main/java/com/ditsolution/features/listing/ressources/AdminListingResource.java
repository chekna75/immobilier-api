package com.ditsolution.features.listing.ressources;

import com.ditsolution.features.auth.entity.RefreshTokenEntity;
import com.ditsolution.features.auth.service.AdminAuditService;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.enums.ListingStatus;
import com.ditsolution.features.listing.repository.ListingRepository;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.spi.HttpRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

@Path("/admin/listings")
@RolesAllowed("ADMIN")
public class AdminListingResource {

    @Inject
    ListingRepository listingRepository;

    @Inject
    SecurityIdentity identity;

    @Inject
    AdminAuditService auditService;

    @Context
    HttpRequest request;

    @PATCH
    @Path("/{id}/remove")
    @Transactional
    public Response removeListing(@PathParam("id") String id) {
        UUID listingId;
        try {
            listingId = UUID.fromString(id);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid listing id").build();
        }

        ListingEntity listing = listingRepository.findById(listingId);
        if(listing == null) {
            throw new NotFoundException("Listing not found");
        }

        if(listing.getStatus() == ListingStatus.REMOVED){
            return Response.noContent().build();
        }

        listing.setStatus(ListingStatus.REMOVED);
        // Log
        auditService.log(
                UUID.fromString(identity.getPrincipal().getName()),
                AdminAuditService.ACTION_LISTING_REMOVE,
                "LISTING",
                listing.getId(),
                null,
                request.getRemoteAddress(),
                request.getHttpHeaders().getHeaderString("User-Agent")
        );

        return Response.noContent().build();
    }
}
