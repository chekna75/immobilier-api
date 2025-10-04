package com.ditsolution.features.messaging.resource;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.messaging.dto.ConversationDto;
import com.ditsolution.features.messaging.dto.CreateConversationRequest;
import com.ditsolution.features.messaging.service.ConversationService;
import com.ditsolution.shared.dto.PagedResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Conversations", description = "Gestion des conversations de messagerie")
public class ConversationResource {
    
    @Inject
    ConversationService conversationService;
    
    @Context
    SecurityContext securityContext;
    
    /**
     * Crée une nouvelle conversation ou retourne une conversation existante
     */
    @POST
    @Operation(summary = "Créer une conversation", description = "Crée une nouvelle conversation ou retourne une conversation existante")
    @RolesAllowed({"USER", "OWNER", "ADMIN"})
    public Response createConversation(@Valid CreateConversationRequest request) {
        try {
            UserEntity currentUser = (UserEntity) securityContext.getUserPrincipal();
            ConversationDto conversation = conversationService.createOrGetConversation(request, currentUser);
            return Response.ok(conversation).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère toutes les conversations de l'utilisateur connecté
     */
    @GET
    @Operation(summary = "Récupérer les conversations", description = "Récupère toutes les conversations de l'utilisateur avec pagination")
    @RolesAllowed({"USER", "OWNER", "ADMIN"})
    public Response getConversations(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        try {
            UserEntity currentUser = (UserEntity) securityContext.getUserPrincipal();
            PagedResponse<ConversationDto> conversations = conversationService.getUserConversations(currentUser, page, size);
            return Response.ok(conversations).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère une conversation spécifique
     */
    @GET
    @Path("/{conversationId}")
    @Operation(summary = "Récupérer une conversation", description = "Récupère une conversation spécifique par son ID")
    @RolesAllowed({"USER", "OWNER", "ADMIN"})
    public Response getConversation(@PathParam("conversationId") Long conversationId) {
        try {
            UserEntity currentUser = (UserEntity) securityContext.getUserPrincipal();
            ConversationDto conversation = conversationService.getConversation(conversationId, currentUser);
            return Response.ok(conversation).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère une conversation par propriété
     */
    @GET
    @Path("/property/{propertyId}")
    @Operation(summary = "Récupérer une conversation par propriété", description = "Récupère une conversation liée à une propriété spécifique")
    @RolesAllowed({"USER", "OWNER", "ADMIN"})
    public Response getConversationByProperty(@PathParam("propertyId") Long propertyId) {
        try {
            UserEntity currentUser = (UserEntity) securityContext.getUserPrincipal();
            ConversationDto conversation = conversationService.getConversationByProperty(propertyId, currentUser);
            return Response.ok(conversation).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Marque les messages d'une conversation comme lus
     */
    @PUT
    @Path("/{conversationId}/read")
    @Operation(summary = "Marquer comme lu", description = "Marque tous les messages d'une conversation comme lus")
    @RolesAllowed({"USER", "OWNER", "ADMIN"})
    public Response markAsRead(@PathParam("conversationId") Long conversationId) {
        try {
            UserEntity currentUser = (UserEntity) securityContext.getUserPrincipal();
            conversationService.markMessagesAsRead(conversationId, currentUser);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Archive une conversation
     */
    @PUT
    @Path("/{conversationId}/archive")
    @Operation(summary = "Archiver une conversation", description = "Archive une conversation")
    @RolesAllowed({"USER", "OWNER", "ADMIN"})
    public Response archiveConversation(@PathParam("conversationId") Long conversationId) {
        try {
            UserEntity currentUser = (UserEntity) securityContext.getUserPrincipal();
            conversationService.archiveConversation(conversationId, currentUser);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Supprime une conversation
     */
    @DELETE
    @Path("/{conversationId}")
    @Operation(summary = "Supprimer une conversation", description = "Supprime définitivement une conversation")
    @RolesAllowed({"USER", "OWNER", "ADMIN"})
    public Response deleteConversation(@PathParam("conversationId") Long conversationId) {
        try {
            UserEntity currentUser = (UserEntity) securityContext.getUserPrincipal();
            conversationService.deleteConversation(conversationId, currentUser);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère le nombre de messages non lus
     */
    @GET
    @Path("/unread-count")
    @Operation(summary = "Nombre de messages non lus", description = "Récupère le nombre total de messages non lus")
    @RolesAllowed({"USER", "OWNER", "ADMIN"})
    public Response getUnreadCount() {
        try {
            UserEntity currentUser = (UserEntity) securityContext.getUserPrincipal();
            Integer unreadCount = conversationService.getUnreadCount(currentUser);
            return Response.ok("{\"unreadCount\": " + unreadCount + "}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Recherche des conversations
     */
    @GET
    @Path("/search")
    @Operation(summary = "Rechercher des conversations", description = "Recherche des conversations par nom d'utilisateur ou titre de propriété")
    @RolesAllowed({"USER", "OWNER", "ADMIN"})
    public Response searchConversations(@QueryParam("q") String searchTerm) {
        try {
            UserEntity currentUser = (UserEntity) securityContext.getUserPrincipal();
            List<ConversationDto> conversations = conversationService.searchConversations(currentUser, searchTerm);
            return Response.ok(conversations).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
}
