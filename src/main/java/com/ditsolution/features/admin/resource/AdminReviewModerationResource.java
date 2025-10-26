package com.ditsolution.features.admin.resource;

import com.ditsolution.features.admin.dto.ModerateReviewRequest;
import com.ditsolution.features.admin.service.AdminReviewModerationService;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.social.entity.ReviewEntity;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/admin/review-moderation")
@RolesAllowed("ADMIN")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminReviewModerationResource {

    @Inject
    AdminReviewModerationService moderationService;
    
    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/queue")
    public Response getModerationQueue() {
        List<ReviewEntity> queue = moderationService.getModerationQueue();
        return Response.ok(queue).build();
    }

    @GET
    @Path("/reported")
    public Response getReportedReviews() {
        List<ReviewEntity> reported = moderationService.getReportedReviews();
        return Response.ok(reported).build();
    }

    @GET
    @Path("/high-reported")
    public Response getHighReportedReviews(@QueryParam("threshold") @DefaultValue("3") int threshold) {
        List<ReviewEntity> highReported = moderationService.getHighReportedReviews(threshold);
        return Response.ok(highReported).build();
    }

    @POST
    @Path("/reviews/{reviewId}")
    public Response moderateReview(
            @PathParam("reviewId") Long reviewId,
            ModerateReviewRequest request,
            @HeaderParam("X-Forwarded-For") String ip,
            @HeaderParam("User-Agent") String userAgent) {
        
        // Récupérer l'admin depuis le contexte de sécurité
        UserEntity admin = getCurrentAdmin();
        
        ReviewEntity result = moderationService.moderateReview(reviewId, admin, request, ip, userAgent);
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
