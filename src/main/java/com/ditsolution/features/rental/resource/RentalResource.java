package com.ditsolution.features.rental.resource;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.rental.dto.*;
import com.ditsolution.features.rental.service.RentalService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/rental")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RentalResource {

    private static final Logger LOG = Logger.getLogger(RentalResource.class);

    @Inject
    RentalService rentalService;

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/contracts")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response createContract(CreateContractRequest request) {
        try {
            UserEntity owner = getCurrentUser();
            RentalContractDto contract = rentalService.createContract(request, owner);
            return Response.ok(contract).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la création du contrat", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/contracts/owner")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response getOwnerContracts() {
        try {
            UserEntity owner = getCurrentUser();
            List<RentalContractDto> contracts = rentalService.getOwnerContracts(owner);
            return Response.ok(contracts).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des contrats", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/contracts/tenant")
    @RolesAllowed({"TENANT", "ADMIN"})
    public Response getTenantContracts() {
        try {
            UserEntity tenant = getCurrentUser();
            List<RentalContractDto> contracts = rentalService.getTenantContracts(tenant);
            return Response.ok(contracts).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des contrats", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/payments/initiate")
    @RolesAllowed({"TENANT", "ADMIN"})
    public Response initiatePayment(InitiatePaymentRequest request) {
        try {
            UserEntity user = getCurrentUser();
            PaymentResponse response = rentalService.initiatePayment(request, user);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de l'initiation du paiement", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/dashboard/owner")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response getOwnerDashboard() {
        try {
            UserEntity owner = getCurrentUser();
            RentalDashboardDto dashboard = rentalService.getOwnerDashboard(owner);
            return Response.ok(dashboard).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération du tableau de bord", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    private UserEntity getCurrentUser() {
        String userId = jwt.getClaim("sub");
        return UserEntity.findById(UUID.fromString(userId));
    }
}
