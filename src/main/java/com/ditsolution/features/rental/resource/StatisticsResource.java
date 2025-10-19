package com.ditsolution.features.rental.resource;

import com.ditsolution.features.rental.dto.StatisticsDto;
import com.ditsolution.features.rental.service.StatisticsService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/api/rental/statistics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class StatisticsResource {

    private static final Logger LOG = Logger.getLogger(StatisticsResource.class);

    @Inject
    StatisticsService statisticsService;

    @GET
    public Response getStatistics(@QueryParam("period") @DefaultValue("month") String period) {
        try {
            // Récupérer l'ID de l'utilisateur connecté depuis le contexte de sécurité
            Long ownerId = getCurrentUserId(); // À implémenter selon votre système d'auth
            
            StatisticsDto statistics = statisticsService.getStatistics(ownerId, period);
            
            return Response.ok(statistics).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des statistiques", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur lors de la récupération des statistiques")
                    .build();
        }
    }

    @GET
    @Path("/export")
    public Response exportStatistics(
            @QueryParam("format") @DefaultValue("pdf") String format,
            @QueryParam("period") @DefaultValue("month") String period) {
        try {
            Long ownerId = getCurrentUserId();
            
            // Générer le fichier d'export selon le format demandé
            byte[] fileData = generateExportFile(ownerId, format, period);
            String fileName = "statistics_" + period + "." + format;
            
            return Response.ok(fileData)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Type", getContentType(format))
                    .build();
        } catch (Exception e) {
            LOG.error("Erreur lors de l'export des statistiques", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur lors de l'export")
                    .build();
        }
    }

    private Long getCurrentUserId() {
        // À implémenter selon votre système d'authentification
        // Retourner l'ID de l'utilisateur connecté
        return 1L; // Exemple
    }

    private byte[] generateExportFile(Long ownerId, String format, String period) {
        // À implémenter selon le format demandé (PDF, Excel, CSV)
        // Retourner les données du fichier
        return new byte[0]; // Exemple
    }

    private String getContentType(String format) {
        return switch (format.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "excel", "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }
}
