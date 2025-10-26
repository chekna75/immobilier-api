package com.ditsolution.features.analytics.resource;

import com.ditsolution.features.analytics.service.AnalyticsQueryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/analytics/stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalyticsStatsResource {

    private static final Logger LOG = Logger.getLogger(AnalyticsStatsResource.class);

    @Inject
    AnalyticsQueryService queryService;

    @GET
    @Path("/listing/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getListingStats(
            @PathParam("listingId") UUID listingId,
            @QueryParam("period") @DefaultValue("30d") String period) {
        try {
            LocalDateTime[] dateRange = getDateRange(period);
            Map<String, Object> stats = queryService.getListingRealTimeStats(listingId, dateRange[0], dateRange[1]);
            return Response.ok(stats).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des statistiques d'annonce", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération des statistiques: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/owner/{ownerId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getOwnerStats(
            @PathParam("ownerId") UUID ownerId,
            @QueryParam("period") @DefaultValue("30d") String period) {
        try {
            LocalDateTime[] dateRange = getDateRange(period);
            Map<String, Object> stats = queryService.getOwnerRealTimeStats(ownerId, dateRange[0], dateRange[1]);
            return Response.ok(stats).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des statistiques de propriétaire", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération des statistiques: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/top-listings/{ownerId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getTopListings(
            @PathParam("ownerId") UUID ownerId,
            @QueryParam("period") @DefaultValue("30d") String period,
            @QueryParam("limit") @DefaultValue("10") int limit) {
        try {
            LocalDateTime[] dateRange = getDateRange(period);
            List<Map<String, Object>> topListings = queryService.getTopListingsByViews(ownerId, dateRange[0], dateRange[1], limit);
            return Response.ok(topListings).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des top annonces", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération des top annonces: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/breakdown/source/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getSourceBreakdown(
            @PathParam("listingId") UUID listingId,
            @QueryParam("period") @DefaultValue("30d") String period) {
        try {
            LocalDateTime[] dateRange = getDateRange(period);
            Map<String, Long> breakdown = queryService.getStatsBySource(listingId, dateRange[0], dateRange[1]);
            return Response.ok(breakdown).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération de la répartition par source", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération de la répartition: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/breakdown/device/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getDeviceBreakdown(
            @PathParam("listingId") UUID listingId,
            @QueryParam("period") @DefaultValue("30d") String period) {
        try {
            LocalDateTime[] dateRange = getDateRange(period);
            Map<String, Long> breakdown = queryService.getStatsByDevice(listingId, dateRange[0], dateRange[1]);
            return Response.ok(breakdown).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération de la répartition par device", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération de la répartition: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/timeseries/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getTimeSeries(
            @PathParam("listingId") UUID listingId,
            @QueryParam("period") @DefaultValue("30d") String period,
            @QueryParam("granularity") @DefaultValue("day") String granularity) {
        try {
            LocalDateTime[] dateRange = getDateRange(period);
            List<Map<String, Object>> timeSeries = queryService.getTimeSeriesStats(listingId, dateRange[0], dateRange[1], granularity);
            return Response.ok(timeSeries).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des séries temporelles", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération des séries temporelles: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    private LocalDateTime[] getDateRange(String period) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate;
        
        switch (period.toLowerCase()) {
            case "7d":
                startDate = endDate.minusDays(7);
                break;
            case "30d":
                startDate = endDate.minusDays(30);
                break;
            case "90d":
                startDate = endDate.minusDays(90);
                break;
            case "1y":
                startDate = endDate.minusYears(1);
                break;
            default:
                startDate = endDate.minusDays(30);
        }
        
        return new LocalDateTime[]{startDate, endDate};
    }
}
