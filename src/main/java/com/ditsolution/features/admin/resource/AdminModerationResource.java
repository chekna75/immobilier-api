package com.ditsolution.features.admin.resource;

import com.ditsolution.features.admin.dto.ModerateListingRequest;
import com.ditsolution.features.admin.service.AdminModerationService;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/admin/moderation")
@RolesAllowed("ADMIN")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminModerationResource {

    @Inject
    AdminModerationService moderationService;
    
    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/queue")
    public Response getModerationQueue() {
        List<ListingEntity> queue = moderationService.getModerationQueue();
        return Response.ok(queue).build();
    }

    @POST
    @Path("/listings/{listingId}")
    public Response moderateListing(
            @PathParam("listingId") UUID listingId,
            ModerateListingRequest request,
            @HeaderParam("X-Forwarded-For") String ip,
            @HeaderParam("User-Agent") String userAgent) {
        
        // Récupérer l'admin depuis le contexte de sécurité
        UserEntity admin = getCurrentAdmin();
        
        ListingEntity result = moderationService.moderateListing(listingId, admin, request, ip, userAgent);
        return Response.ok(result).build();
    }

    private UserEntity getCurrentAdmin() {
        var principal = identity == null ? null : identity.getPrincipal();
        if (principal == null) return null;
        try { 
            return UserEntity.findById(UUID.fromString(principal.getName())); 
        } catch (Exception e) { 
            return null; 
        }
    }
}
