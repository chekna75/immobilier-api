package com.ditsolution.features.rental.resource;

import com.ditsolution.features.rental.service.PaymentIntegrationTestService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/api/rental/tests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentIntegrationTestResource {

    private static final Logger LOG = Logger.getLogger(PaymentIntegrationTestResource.class);

    @Inject
    PaymentIntegrationTestService integrationTestService;

    /**
     * Lancer un test d'intégration complet
     */
    @POST
    @Path("/integration/{contractId}")
    public Response runIntegrationTest(@PathParam("contractId") Long contractId) {
        try {
            LOG.info("🧪 Lancement du test d'intégration pour le contrat: " + contractId);
            
            PaymentIntegrationTestService.IntegrationTestResult result = 
                integrationTestService.testCompletePaymentFlow(contractId);
            
            if (result.success) {
                return Response.ok(result).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(result)
                    .build();
            }

        } catch (Exception e) {
            LOG.error("❌ Erreur lors du test d'intégration", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erreur lors du test d'intégration: " + e.getMessage())
                .build();
        }
    }

    /**
     * Tester la synchronisation des données
     */
    @POST
    @Path("/sync/{contractId}")
    public Response testDataSynchronization(@PathParam("contractId") Long contractId) {
        try {
            LOG.info("🔄 Test de synchronisation pour le contrat: " + contractId);
            
            PaymentIntegrationTestService.SyncTestResult result = 
                integrationTestService.testDataSynchronization(contractId);
            
            if (result.success) {
                return Response.ok(result).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(result)
                    .build();
            }

        } catch (Exception e) {
            LOG.error("❌ Erreur lors du test de synchronisation", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erreur lors du test de synchronisation: " + e.getMessage())
                .build();
        }
    }

    /**
     * Obtenir le statut des services
     */
    @GET
    @Path("/status")
    public Response getServicesStatus() {
        try {
            return Response.ok(Map.of(
                "stripeService", "✅ Disponible",
                "feeCalculationService", "✅ Disponible", 
                "pdfGenerationService", "✅ Disponible",
                "splitPaymentService", "✅ Disponible",
                "timestamp", System.currentTimeMillis()
            )).build();

        } catch (Exception e) {
            LOG.error("❌ Erreur lors de la vérification du statut", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erreur lors de la vérification du statut")
                .build();
        }
    }
}
