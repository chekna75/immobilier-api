package com.ditsolution.features.messaging.resource;

import com.ditsolution.features.messaging.dto.MessageDto;
import com.ditsolution.features.messaging.service.MessageService;
import com.ditsolution.shared.dto.PagedResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;

@Path("/admin/conversations/{conversationId}/messages")
@Tag(name = "Admin - Messages", description = "Endpoints d'administration pour la gestion des messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AdminMessageResource {
    
    @Inject
    MessageService messageService;
    
    /**
     * Récupère tous les messages d'une conversation (pour les administrateurs)
     */
    @GET
    @Operation(summary = "Récupérer tous les messages d'une conversation", description = "Récupère tous les messages d'une conversation pour la modération")
    public Response getMessages(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size) {
        try {
            PagedResponse<MessageDto> messages = messageService.getMessagesForAdmin(conversationId, page, size);
            return Response.ok(messages).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère un message spécifique (pour les administrateurs)
     */
    @GET
    @Path("/{messageId}")
    @Operation(summary = "Récupérer un message", description = "Récupère un message spécifique pour la modération")
    public Response getMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        try {
            MessageDto message = messageService.getMessageForAdmin(conversationId, messageId);
            return Response.ok(message).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Supprime un message (pour les administrateurs)
     */
    @DELETE
    @Path("/{messageId}")
    @Operation(summary = "Supprimer un message", description = "Supprime un message pour la modération")
    public Response deleteMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        try {
            messageService.deleteMessageForAdmin(conversationId, messageId);
            return Response.ok("{\"message\": \"Message supprimé avec succès\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère les messages d'une conversation après une date donnée (pour les administrateurs)
     */
    @GET
    @Path("/since")
    @Operation(summary = "Récupérer les messages depuis une date", description = "Récupère les messages d'une conversation depuis une date donnée")
    public Response getMessagesSince(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("since") String sinceDate) {
        try {
            LocalDateTime since = LocalDateTime.parse(sinceDate);
            var messages = messageService.getMessagesSinceForAdmin(conversationId, since);
            return Response.ok(messages).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère les statistiques des messages d'une conversation
     */
    @GET
    @Path("/stats")
    @Operation(summary = "Statistiques des messages", description = "Récupère les statistiques des messages d'une conversation")
    public Response getMessageStats(@PathParam("conversationId") Long conversationId) {
        try {
            var stats = messageService.getMessageStatsForAdmin(conversationId);
            return Response.ok(stats).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
}
