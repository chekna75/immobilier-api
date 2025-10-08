package com.ditsolution.features.messaging.resource;

import com.ditsolution.features.messaging.dto.ConversationDto;
import com.ditsolution.features.messaging.service.ConversationService;
import com.ditsolution.shared.dto.PagedResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin/conversations")
@Tag(name = "Admin - Conversations", description = "Endpoints d'administration pour la gestion des conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AdminConversationResource {
    
    @Inject
    ConversationService conversationService;
    
    /**
     * Récupère toutes les conversations (pour les administrateurs)
     */
    @GET
    @Operation(summary = "Récupérer toutes les conversations", description = "Récupère toutes les conversations du système pour la modération")
    public Response getAllConversations(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String searchTerm,
            @QueryParam("archived") Boolean archived) {
        try {
            PagedResponse<ConversationDto> conversations = conversationService
                .getAllConversationsForAdmin(page, size, searchTerm, archived);
            return Response.ok(conversations).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère une conversation spécifique (pour les administrateurs)
     */
    @GET
    @Path("/{conversationId}")
    @Operation(summary = "Récupérer une conversation", description = "Récupère une conversation spécifique pour la modération")
    public Response getConversation(@PathParam("conversationId") Long conversationId) {
        try {
            ConversationDto conversation = conversationService.getConversationForAdmin(conversationId);
            return Response.ok(conversation).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Archive une conversation (pour les administrateurs)
     */
    @PUT
    @Path("/{conversationId}/archive")
    @Operation(summary = "Archiver une conversation", description = "Archive une conversation pour la modération")
    public Response archiveConversation(@PathParam("conversationId") Long conversationId) {
        try {
            conversationService.archiveConversationForAdmin(conversationId);
            return Response.ok("{\"message\": \"Conversation archivée avec succès\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Désarchive une conversation (pour les administrateurs)
     */
    @PUT
    @Path("/{conversationId}/unarchive")
    @Operation(summary = "Désarchiver une conversation", description = "Désarchive une conversation pour la modération")
    public Response unarchiveConversation(@PathParam("conversationId") Long conversationId) {
        try {
            conversationService.unarchiveConversationForAdmin(conversationId);
            return Response.ok("{\"message\": \"Conversation désarchivée avec succès\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Désactive une conversation (pour les administrateurs)
     */
    @PUT
    @Path("/{conversationId}/deactivate")
    @Operation(summary = "Désactiver une conversation", description = "Désactive une conversation pour la modération")
    public Response deactivateConversation(@PathParam("conversationId") Long conversationId) {
        try {
            conversationService.deactivateConversationForAdmin(conversationId);
            return Response.ok("{\"message\": \"Conversation désactivée avec succès\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Active une conversation (pour les administrateurs)
     */
    @PUT
    @Path("/{conversationId}/activate")
    @Operation(summary = "Activer une conversation", description = "Active une conversation pour la modération")
    public Response activateConversation(@PathParam("conversationId") Long conversationId) {
        try {
            conversationService.activateConversationForAdmin(conversationId);
            return Response.ok("{\"message\": \"Conversation activée avec succès\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère les statistiques des conversations
     */
    @GET
    @Path("/stats")
    @Operation(summary = "Statistiques des conversations", description = "Récupère les statistiques des conversations pour le tableau de bord admin")
    public Response getConversationStats() {
        try {
            var stats = conversationService.getConversationStatsForAdmin();
            return Response.ok(stats).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
}
