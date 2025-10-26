package com.ditsolution.features.rental.resource;

import com.ditsolution.features.rental.dto.CreateSplitPaymentRequest;
import com.ditsolution.features.rental.dto.SplitPaymentDto;
import com.ditsolution.features.rental.dto.SplitPaymentItemDto;
import com.ditsolution.features.rental.entity.SplitPaymentEntity;
import com.ditsolution.features.rental.entity.SplitPaymentItemEntity;
import com.ditsolution.features.rental.service.SplitPaymentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

@Path("/api/rental/split-payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SplitPaymentResource {

    private static final Logger LOG = Logger.getLogger(SplitPaymentResource.class);

    @Inject
    SplitPaymentService splitPaymentService;

    /**
     * Créer un paiement fractionné
     */
    @POST
    public Response createSplitPayment(CreateSplitPaymentRequest request) {
        try {
            LOG.info("Création d'un paiement fractionné pour le contrat: " + request.contractId);
            
            SplitPaymentEntity splitPayment = splitPaymentService.createSplitPayment(
                request.contractId,
                request.totalAmount,
                request.depositPercentage != null ? request.depositPercentage : 30,
                request.description
            );

            SplitPaymentDto dto = convertToDto(splitPayment);
            return Response.ok(dto).build();

        } catch (Exception e) {
            LOG.error("Erreur lors de la création du paiement fractionné", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Erreur: " + e.getMessage())
                .build();
        }
    }

    /**
     * Obtenir les paiements fractionnés d'un contrat
     */
    @GET
    @Path("/contract/{contractId}")
    public Response getSplitPaymentsByContract(@PathParam("contractId") Long contractId) {
        try {
            List<SplitPaymentEntity> splitPayments = splitPaymentService.getSplitPaymentsByContract(contractId);
            List<SplitPaymentDto> dtos = splitPayments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

            return Response.ok(dtos).build();

        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des paiements fractionnés", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erreur lors de la récupération des paiements fractionnés")
                .build();
        }
    }

    /**
     * Obtenir un paiement fractionné par ID
     */
    @GET
    @Path("/{splitPaymentId}")
    public Response getSplitPaymentById(@PathParam("splitPaymentId") Long splitPaymentId) {
        try {
            return splitPaymentService.getSplitPaymentById(splitPaymentId)
                .map(splitPayment -> Response.ok(convertToDto(splitPayment)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                    .entity("Paiement fractionné non trouvé")
                    .build());

        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération du paiement fractionné", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erreur lors de la récupération du paiement fractionné")
                .build();
        }
    }

    /**
     * Annuler un paiement fractionné
     */
    @DELETE
    @Path("/{splitPaymentId}")
    public Response cancelSplitPayment(@PathParam("splitPaymentId") Long splitPaymentId) {
        try {
            splitPaymentService.cancelSplitPayment(splitPaymentId);
            return Response.ok("Paiement fractionné annulé avec succès").build();

        } catch (Exception e) {
            LOG.error("Erreur lors de l'annulation du paiement fractionné", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Erreur: " + e.getMessage())
                .build();
        }
    }

    /**
     * Obtenir les statistiques des paiements fractionnés
     */
    @GET
    @Path("/statistics/{contractId}")
    public Response getSplitPaymentStatistics(@PathParam("contractId") Long contractId) {
        try {
            SplitPaymentService.SplitPaymentStatistics stats = 
                splitPaymentService.getSplitPaymentStatistics(contractId);
            return Response.ok(stats).build();

        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des statistiques", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erreur lors de la récupération des statistiques")
                .build();
        }
    }

    /**
     * Traiter le paiement d'un élément
     */
    @POST
    @Path("/items/{itemId}/process")
    public Response processPaymentItem(@PathParam("itemId") Long itemId, 
                                     @QueryParam("stripePaymentIntentId") String stripePaymentIntentId) {
        try {
            splitPaymentService.processPaymentItem(itemId, stripePaymentIntentId);
            return Response.ok("Paiement traité avec succès").build();

        } catch (Exception e) {
            LOG.error("Erreur lors du traitement du paiement", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Erreur: " + e.getMessage())
                .build();
        }
    }

    /**
     * Convertir une entité en DTO
     */
    private SplitPaymentDto convertToDto(SplitPaymentEntity entity) {
        List<SplitPaymentItemDto> itemDtos = entity.getPaymentItems() != null ?
            entity.getPaymentItems().stream()
                .map(this::convertItemToDto)
                .collect(Collectors.toList()) : List.of();

        return new SplitPaymentDto(
            entity.id,
            entity.getContract().id,
            entity.getTotalAmount(),
            entity.getDepositPercentage(),
            entity.getDepositAmount(),
            entity.getBalanceAmount(),
            entity.getStatus().toString(),
            entity.getDescription(),
            entity.getCreatedAt().toLocalDate(),
            itemDtos
        );
    }

    /**
     * Convertir un élément de paiement en DTO
     */
    private SplitPaymentItemDto convertItemToDto(SplitPaymentItemEntity item) {
        return new SplitPaymentItemDto(
            item.id,
            item.getPaymentType().toString(),
            item.getAmount(),
            item.getDueDate(),
            item.getPaidDate(),
            item.getStatus().toString(),
            item.getPaymentMethod(),
            item.getTransactionId(),
            item.getReceiptUrl(),
            item.getNotes()
        );
    }
}
