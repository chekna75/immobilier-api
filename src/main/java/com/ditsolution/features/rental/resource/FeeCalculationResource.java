package com.ditsolution.features.rental.resource;

import com.ditsolution.features.rental.service.FeeCalculationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.Map;

@Path("/api/rental/fees")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FeeCalculationResource {

    private static final Logger LOG = Logger.getLogger(FeeCalculationResource.class);

    @Inject
    FeeCalculationService feeCalculationService;

    /**
     * Calculer les frais pour un montant donné
     */
    @POST
    @Path("/calculate")
    public Response calculateFees(@QueryParam("amount") BigDecimal amount,
                                @QueryParam("paymentType") @DefaultValue("rent") String paymentType) {
        try {
            LOG.info("Calcul des frais pour montant: " + amount + ", type: " + paymentType);
            
            FeeCalculationService.FeeCalculationResult result = 
                feeCalculationService.calculateFees(amount, paymentType);
            
            return Response.ok(result).build();

        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des frais", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Erreur: " + e.getMessage())
                .build();
        }
    }

    /**
     * Calculer les frais pour un paiement fractionné
     */
    @POST
    @Path("/calculate-split")
    public Response calculateSplitPaymentFees(@QueryParam("totalAmount") BigDecimal totalAmount,
                                           @QueryParam("depositPercentage") @DefaultValue("30") Integer depositPercentage) {
        try {
            LOG.info("Calcul des frais pour paiement fractionné - Montant: " + totalAmount + 
                    ", Pourcentage acompte: " + depositPercentage);
            
            FeeCalculationService.SplitPaymentFeeResult result = 
                feeCalculationService.calculateSplitPaymentFees(totalAmount, depositPercentage);
            
            return Response.ok(result).build();

        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des frais de paiement fractionné", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Erreur: " + e.getMessage())
                .build();
        }
    }

    /**
     * Obtenir la structure des frais pour un type de paiement
     */
    @GET
    @Path("/structure/{paymentType}")
    public Response getFeeStructure(@PathParam("paymentType") String paymentType) {
        try {
            FeeCalculationService.FeeStructure structure = 
                feeCalculationService.getFeeStructure(paymentType);
            
            return Response.ok(structure).build();

        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération de la structure des frais", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erreur lors de la récupération de la structure des frais")
                .build();
        }
    }

    /**
     * Obtenir toutes les structures de frais
     */
    @GET
    @Path("/structures")
    public Response getAllFeeStructures() {
        try {
            Map<String, FeeCalculationService.FeeStructure> structures = Map.of(
                "rent", feeCalculationService.getFeeStructure("rent"),
                "deposit", feeCalculationService.getFeeStructure("deposit"),
                "maintenance", feeCalculationService.getFeeStructure("maintenance")
            );
            
            return Response.ok(structures).build();

        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des structures de frais", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erreur lors de la récupération des structures de frais")
                .build();
        }
    }

    /**
     * Simuler différents scénarios de frais
     */
    @POST
    @Path("/simulate")
    public Response simulateFeeScenarios(@QueryParam("amount") BigDecimal amount) {
        try {
            LOG.info("Simulation des frais pour montant: " + amount);
            
            Map<String, FeeCalculationService.FeeCalculationResult> scenarios = Map.of(
                "rent", feeCalculationService.calculateFees(amount, "rent"),
                "deposit", feeCalculationService.calculateFees(amount, "deposit"),
                "maintenance", feeCalculationService.calculateFees(amount, "maintenance")
            );
            
            return Response.ok(scenarios).build();

        } catch (Exception e) {
            LOG.error("Erreur lors de la simulation des frais", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Erreur: " + e.getMessage())
                .build();
        }
    }
}
