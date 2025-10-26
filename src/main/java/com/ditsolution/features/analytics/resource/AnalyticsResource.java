package com.ditsolution.features.analytics.resource;

import com.ditsolution.features.analytics.dto.*;
import com.ditsolution.features.analytics.entity.*;
import com.ditsolution.features.analytics.service.AnalyticsService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/api/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalyticsResource {

    private static final Logger LOG = Logger.getLogger(AnalyticsResource.class);

    @Inject
    AnalyticsService analyticsService;

    @POST
    @Path("/views")
    @RolesAllowed({"USER", "ADMIN"})
    public Response trackView(@Valid AnalyticsViewDto dto) {
        try {
            AnalyticsViewEntity entity = analyticsService.trackView(dto);
            return Response.ok(entity).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking de la vue", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors du tracking de la vue: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/clicks")
    @RolesAllowed({"USER", "ADMIN"})
    public Response trackClick(@Valid AnalyticsClickDto dto) {
        try {
            AnalyticsClickEntity entity = analyticsService.trackClick(dto);
            return Response.ok(entity).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking du clic", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors du tracking du clic: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/favorites")
    @RolesAllowed({"USER", "ADMIN"})
    public Response trackFavorite(@Valid AnalyticsFavoriteDto dto) {
        try {
            AnalyticsFavoriteEntity entity = analyticsService.trackFavorite(dto);
            return Response.ok(entity).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking du favori", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors du tracking du favori: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/contacts")
    @RolesAllowed({"USER", "ADMIN"})
    public Response trackContact(@Valid AnalyticsContactDto dto) {
        try {
            AnalyticsContactEntity entity = analyticsService.trackContact(dto);
            return Response.ok(entity).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking du contact", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors du tracking du contact: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/searches")
    @RolesAllowed({"USER", "ADMIN"})
    public Response trackSearch(@Valid AnalyticsSearchDto dto) {
        try {
            AnalyticsSearchEntity entity = analyticsService.trackSearch(dto);
            return Response.ok(entity).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking de la recherche", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors du tracking de la recherche: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/conversions")
    @RolesAllowed({"USER", "ADMIN"})
    public Response trackConversion(@Valid AnalyticsConversionDto dto) {
        try {
            AnalyticsConversionEntity entity = analyticsService.trackConversion(dto);
            return Response.ok(entity).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking de la conversion", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors du tracking de la conversion: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/views/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getViewsByListing(
            @PathParam("listingId") String listingId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {
        try {
            // TODO: Implémenter la logique de récupération des vues
            return Response.ok("{\"message\": \"Fonctionnalité en cours de développement\"}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des vues", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération des vues: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/clicks/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getClicksByListing(
            @PathParam("listingId") String listingId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {
        try {
            // TODO: Implémenter la logique de récupération des clics
            return Response.ok("{\"message\": \"Fonctionnalité en cours de développement\"}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des clics", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération des clics: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/favorites/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getFavoritesByListing(
            @PathParam("listingId") String listingId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {
        try {
            // TODO: Implémenter la logique de récupération des favoris
            return Response.ok("{\"message\": \"Fonctionnalité en cours de développement\"}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des favoris", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération des favoris: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/contacts/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getContactsByListing(
            @PathParam("listingId") String listingId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {
        try {
            // TODO: Implémenter la logique de récupération des contacts
            return Response.ok("{\"message\": \"Fonctionnalité en cours de développement\"}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des contacts", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération des contacts: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/conversions/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getConversionsByListing(
            @PathParam("listingId") String listingId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {
        try {
            // TODO: Implémenter la logique de récupération des conversions
            return Response.ok("{\"message\": \"Fonctionnalité en cours de développement\"}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des conversions", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération des conversions: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
