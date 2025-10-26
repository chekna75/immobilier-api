package com.ditsolution.features.analytics.resource;

import com.ditsolution.features.analytics.dto.AnalyticsReportDto;
import com.ditsolution.features.analytics.service.AnalyticsReportService;
import com.ditsolution.features.analytics.service.AnalyticsExportService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.UUID;

@Path("/api/analytics/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalyticsReportResource {

    private static final Logger LOG = Logger.getLogger(AnalyticsReportResource.class);

    @Inject
    AnalyticsReportService reportService;

    @Inject
    AnalyticsExportService exportService;

    @GET
    @Path("/listing/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response generateListingReport(
            @PathParam("listingId") UUID listingId,
            @QueryParam("period") @DefaultValue("30d") String period) {
        try {
            AnalyticsReportDto report = reportService.generateListingReport(listingId, period);
            return Response.ok(report).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la génération du rapport d'annonce", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la génération du rapport: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/owner/{ownerId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response generateOwnerReport(
            @PathParam("ownerId") UUID ownerId,
            @QueryParam("period") @DefaultValue("30d") String period) {
        try {
            AnalyticsReportDto report = reportService.generateOwnerReport(ownerId, period);
            return Response.ok(report).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la génération du rapport de propriétaire", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la génération du rapport: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/export/listing/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response exportListingReport(
            @PathParam("listingId") UUID listingId,
            @QueryParam("period") @DefaultValue("30d") String period,
            @QueryParam("format") @DefaultValue("pdf") String format) {
        try {
            // Générer le rapport
            AnalyticsReportDto report = reportService.generateListingReport(listingId, period);
            
            // Exporter selon le format
            byte[] fileData = switch (format.toLowerCase()) {
                case "csv" -> exportService.exportToCSV(report);
                case "json" -> exportService.exportToJSON(report);
                case "pdf" -> exportService.exportToPDF(report);
                case "excel", "xlsx" -> exportService.exportToExcel(report);
                default -> throw new IllegalArgumentException("Format non supporté: " + format);
            };
            
            String fileName = exportService.generateFileName("listing", period, format);
            String contentType = exportService.getContentType(format);
            
            return Response.ok(fileData)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Type", contentType)
                    .build();
        } catch (Exception e) {
            LOG.error("Erreur lors de l'export du rapport d'annonce", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de l'export: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/export/owner/{ownerId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response exportOwnerReport(
            @PathParam("ownerId") UUID ownerId,
            @QueryParam("period") @DefaultValue("30d") String period,
            @QueryParam("format") @DefaultValue("pdf") String format) {
        try {
            // Générer le rapport
            AnalyticsReportDto report = reportService.generateOwnerReport(ownerId, period);
            
            // Exporter selon le format
            byte[] fileData = switch (format.toLowerCase()) {
                case "csv" -> exportService.exportToCSV(report);
                case "json" -> exportService.exportToJSON(report);
                case "pdf" -> exportService.exportToPDF(report);
                case "excel", "xlsx" -> exportService.exportToExcel(report);
                default -> throw new IllegalArgumentException("Format non supporté: " + format);
            };
            
            String fileName = exportService.generateFileName("owner", period, format);
            String contentType = exportService.getContentType(format);
            
            return Response.ok(fileData)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Type", contentType)
                    .build();
        } catch (Exception e) {
            LOG.error("Erreur lors de l'export du rapport de propriétaire", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de l'export: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/stats/listing/{listingId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getListingStats(
            @PathParam("listingId") UUID listingId,
            @QueryParam("period") @DefaultValue("30d") String period) {
        try {
            // TODO: Implémenter les statistiques rapides
            return Response.ok("{\"message\": \"Fonctionnalité de statistiques rapides en cours de développement\"}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des statistiques d'annonce", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération des statistiques: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/stats/owner/{ownerId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getOwnerStats(
            @PathParam("ownerId") UUID ownerId,
            @QueryParam("period") @DefaultValue("30d") String period) {
        try {
            // TODO: Implémenter les statistiques rapides
            return Response.ok("{\"message\": \"Fonctionnalité de statistiques rapides en cours de développement\"}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des statistiques de propriétaire", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erreur lors de la récupération des statistiques: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
