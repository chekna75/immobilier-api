package com.ditsolution.features.auth.resource;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.auth.entity.RefreshTokenEntity;
import com.ditsolution.features.auth.entity.RoleChangeRequestEntity;
import com.ditsolution.features.auth.service.AdminAuditService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.spi.HttpRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Path("/admin/users")
@RolesAllowed("ADMIN") // 🔒 au niveau classe pour éviter l’oubli
@Produces(MediaType.APPLICATION_JSON)
public class AdminUserResource {

    @Inject SecurityIdentity identity;
    @Inject
    AdminAuditService auditService;

    @Context
    HttpRequest request;

    @PATCH
    @Path("/{id}/suspend")
    @Transactional
    public Response suspend(@PathParam("id") String id) {
        UUID userId = parseUuidOrBadRequest(id);
        // 🔒 empêcher l’auto-suspension
        if (isSelf(userId)) return forbidden("Impossible de suspendre votre propre compte.");

        UserEntity u = UserEntity.findById(userId);
        if (u == null) return Response.status(Response.Status.NOT_FOUND).build();

        if (u.status == UserEntity.Status.SUSPENDED) {
            return Response.noContent().build(); // idempotent
        }
        u.status = UserEntity.Status.SUSPENDED;

        // 🔒 révoquer les refresh tokens actifs
        RefreshTokenEntity.update("revokedAt = ?1 WHERE user = ?2 AND revokedAt IS NULL",
                OffsetDateTime.now(), u);

        String ip = request.getRemoteAddress();
        String userAgent = request.getHttpHeaders().getHeaderString("User-Agent");

        // Audit
        auditService.log(
                UUID.fromString(identity.getPrincipal().getName()),
                AdminAuditService.ACTION_USER_SUSPEND,
                "USER",
                u.id,
                null,
                ip,
                userAgent
        );

        // TODO: audit log (adminId, targetUserId, action, ip/ua)
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}/activate")
    @Transactional
    public Response activate(@PathParam("id") String id) {
        UUID userId = parseUuidOrBadRequest(id);
        UserEntity u = UserEntity.findById(userId);
        if (u == null) return Response.status(Response.Status.NOT_FOUND).build();

        if (u.status == UserEntity.Status.ACTIVE) {
            return Response.noContent().build(); // idempotent
        }
        u.status = UserEntity.Status.ACTIVE;

        // (Optionnel) ne pas réactiver automatiquement les refresh tokens révoqués
        // → l’utilisateur devra se reconnecter

        // Audit
        String ip = request.getRemoteAddress();
        String userAgent = request.getHttpHeaders().getHeaderString("User-Agent");

        auditService.log(
                UUID.fromString(identity.getPrincipal().getName()),
                AdminAuditService.ACTION_USER_ACTIVATE,
                "USER",
                u.id,
                null,
                ip,
                userAgent
        );

        // TODO: audit log
        return Response.noContent().build();
    }

    // ---------- helpers ----------
    private UUID parseUuidOrBadRequest(String raw) {
        try { return UUID.fromString(raw); }
        catch (Exception e) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorDto("INVALID_ID","id doit être un UUID")).build());
        }
    }

    private boolean isSelf(UUID target) {
        try {
            var sub = identity.getPrincipal().getName();
            return target.equals(UUID.fromString(sub));
        } catch (Exception e) { return false; }
    }

    record ErrorDto(String error, String message) {}

    private Response forbidden(String message) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorDto("FORBIDDEN", message))
                .build();
    }

    // ===== ENDPOINTS POUR LA GESTION DES DEMANDES DE CHANGEMENT DE RÔLE =====

    @GET
    @Path("/role-change-requests")
    public Response getRoleChangeRequests(
            @QueryParam("status") String status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        String query = "1=1";
        if (status != null && !status.isBlank()) {
            query += " AND status = '" + status.toUpperCase() + "'";
        }
        
        List<RoleChangeRequestEntity> requests = RoleChangeRequestEntity.find(query)
            .page(page, size)
            .list();
            
        List<RoleChangeRequestDto> dtos = requests.stream()
            .map(this::toDto)
            .toList();
            
        return Response.ok(new RoleChangeRequestsResponse(dtos, requests.size())).build();
    }

    @GET
    @Path("/role-change-requests/{id}")
    public Response getRoleChangeRequest(@PathParam("id") Long id) {
        RoleChangeRequestEntity request = RoleChangeRequestEntity.findById(id);
        if (request == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toDto(request)).build();
    }

    @PATCH
    @Path("/role-change-requests/{id}/approve")
    @Transactional
    public Response approveRoleChangeRequest(@PathParam("id") Long id, @QueryParam("adminNotes") String adminNotes) {
        return processRoleChangeRequest(id, "APPROVED", adminNotes);
    }

    @PATCH
    @Path("/role-change-requests/{id}/reject")
    @Transactional
    public Response rejectRoleChangeRequest(@PathParam("id") Long id, @QueryParam("adminNotes") String adminNotes) {
        return processRoleChangeRequest(id, "REJECTED", adminNotes);
    }

    private Response processRoleChangeRequest(Long id, String newStatus, String adminNotes) {
        RoleChangeRequestEntity request = RoleChangeRequestEntity.findById(id);
        if (request == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!"PENDING".equals(request.getStatus())) {
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorDto("ALREADY_PROCESSED", "Cette demande a déjà été traitée"))
                .build();
        }

        UserEntity admin = getCurrentUser();
        request.setStatus(newStatus);
        request.setAdminNotes(adminNotes);
        request.setProcessedAt(OffsetDateTime.now());
        request.setProcessedBy(admin);

        if ("APPROVED".equals(newStatus)) {
            // Changer le rôle de l'utilisateur
            UserEntity user = request.getUser();
            try {
                user.setRole(UserEntity.Role.valueOf(request.getRequestedRole()));
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("INVALID_ROLE", "Rôle invalide: " + request.getRequestedRole()))
                    .build();
            }
            
            // Révoquer tous les refresh tokens de l'utilisateur
            RefreshTokenEntity.update("revokedAt = ?1 WHERE user = ?2 AND revokedAt IS NULL",
                OffsetDateTime.now(), user);
        }

        // Log de l'action admin
        String action = "APPROVED".equals(newStatus) ? AdminAuditService.ACTION_ROLE_APPROVE : AdminAuditService.ACTION_ROLE_REJECT;
        auditService.log(
            action,
            "Demande de changement de rôle " + newStatus.toLowerCase(),
            request.getUser().getId(),
            getClientIp(),
            getUserAgent()
        );

        return Response.ok(toDto(request)).build();
    }

    private UserEntity getCurrentUser() {
        String email = identity.getPrincipal().getName();
        return UserEntity.find("email", email).firstResult();
    }

    private String getClientIp() {
        String xff = request.getHttpHeaders().getHeaderString("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int idx = xff.indexOf(',');
            return (idx >= 0 ? xff.substring(0, idx) : xff).trim();
        }
        return request.getRemoteAddress();
    }

    private String getUserAgent() {
        return request.getHttpHeaders().getHeaderString("User-Agent");
    }

    private RoleChangeRequestDto toDto(RoleChangeRequestEntity request) {
        return new RoleChangeRequestDto(
            request.id,
            request.getUser().getId(),
            request.getUser().getEmail(),
            request.getUser().getFirstName() + " " + request.getUser().getLastName(),
            request.getRequestedRole(),
            request.getStatus(),
            request.getReason(),
            request.getAdminNotes(),
            request.getCreatedAt(),
            request.getProcessedAt(),
            request.getProcessedBy() != null ? request.getProcessedBy().getId() : null
        );
    }

    // DTOs pour les réponses
    public record RoleChangeRequestDto(
        Long id,
        UUID userId,
        String userEmail,
        String userName,
        String requestedRole,
        String status,
        String reason,
        String adminNotes,
        OffsetDateTime createdAt,
        OffsetDateTime processedAt,
        UUID processedBy
    ) {}

    public record RoleChangeRequestsResponse(List<RoleChangeRequestDto> requests, int total) {}

}
