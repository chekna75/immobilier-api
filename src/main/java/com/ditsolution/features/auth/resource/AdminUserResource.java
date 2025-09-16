package com.ditsolution.features.auth.resource;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.auth.entity.RefreshTokenEntity;
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
    
    

}
