package com.ditsolution.features.messaging.resource;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.messaging.dto.MessageDto;
import com.ditsolution.features.messaging.dto.SendMessageRequest;
import com.ditsolution.features.messaging.service.MessageService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Path("/conversations/{conversationId}/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Messages", description = "Gestion des messages de messagerie")
public class MessageResource {
    
    @Inject
    MessageService messageService;
    
    @Context
    SecurityContext securityContext;
    
    /**
     * Envoie un message dans une conversation
     */
    @POST
    @Operation(summary = "Envoyer un message", description = "Envoie un nouveau message dans une conversation")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response sendMessage(
            @PathParam("conversationId") Long conversationId,
            @Valid SendMessageRequest request) {
        try {
            String userId = securityContext.getUserPrincipal().getName();
            UserEntity currentUser = UserEntity.findById(UUID.fromString(userId));
            MessageDto message = messageService.sendMessage(conversationId, request, currentUser);
            return Response.ok(message).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère les messages d'une conversation
     */
    @GET
    @Operation(summary = "Récupérer les messages", description = "Récupère les messages d'une conversation avec pagination")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response getMessages(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size) {
        try {
            String userId = securityContext.getUserPrincipal().getName();
            UserEntity currentUser = UserEntity.findById(UUID.fromString(userId));
            PagedResponse<MessageDto> messages = messageService.getMessages(conversationId, currentUser, page, size);
            return Response.ok(messages).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère les messages récents d'une conversation
     */
    @GET
    @Path("/recent")
    @Operation(summary = "Messages récents", description = "Récupère les messages récents d'une conversation")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response getRecentMessages(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        try {
            String userId = securityContext.getUserPrincipal().getName();
            UserEntity currentUser = UserEntity.findById(UUID.fromString(userId));
            List<MessageDto> messages = messageService.getRecentMessages(conversationId, currentUser, limit);
            return Response.ok(messages).build();
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
    @Path("/read")
    @Operation(summary = "Marquer comme lu", description = "Marque tous les messages d'une conversation comme lus")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response markAsRead(@PathParam("conversationId") Long conversationId) {
        try {
            String userId = securityContext.getUserPrincipal().getName();
            UserEntity currentUser = UserEntity.findById(UUID.fromString(userId));
            messageService.markMessagesAsRead(conversationId, currentUser);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère le nombre de messages non lus dans une conversation
     */
    @GET
    @Path("/unread-count")
    @Operation(summary = "Nombre de messages non lus", description = "Récupère le nombre de messages non lus dans une conversation")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response getUnreadCount(@PathParam("conversationId") Long conversationId) {
        try {
            String userId = securityContext.getUserPrincipal().getName();
            UserEntity currentUser = UserEntity.findById(UUID.fromString(userId));
            Long unreadCount = messageService.getUnreadCountInConversation(conversationId, currentUser);
            return Response.ok("{\"unreadCount\": " + unreadCount + "}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère tous les messages non lus de l'utilisateur
     */
    @GET
    @Path("/unread")
    @Operation(summary = "Messages non lus", description = "Récupère tous les messages non lus de l'utilisateur")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response getUnreadMessages() {
        try {
            String userId = securityContext.getUserPrincipal().getName();
            UserEntity currentUser = UserEntity.findById(UUID.fromString(userId));
            List<MessageDto> messages = messageService.getUnreadMessages(currentUser);
            return Response.ok(messages).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère le nombre total de messages non lus
     */
    @GET
    @Path("/total-unread-count")
    @Operation(summary = "Nombre total de messages non lus", description = "Récupère le nombre total de messages non lus de l'utilisateur")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response getTotalUnreadCount() {
        try {
            String userId = securityContext.getUserPrincipal().getName();
            UserEntity currentUser = UserEntity.findById(UUID.fromString(userId));
            Long unreadCount = messageService.getTotalUnreadCount(currentUser);
            return Response.ok("{\"unreadCount\": " + unreadCount + "}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * Récupère les messages d'une conversation après une date donnée
     */
    @GET
    @Path("/since")
    @Operation(summary = "Messages depuis une date", description = "Récupère les messages d'une conversation après une date donnée")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response getMessagesSince(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("since") String since) {
        try {
            String userId = securityContext.getUserPrincipal().getName();
            UserEntity currentUser = UserEntity.findById(UUID.fromString(userId));
            LocalDateTime sinceDate = LocalDateTime.parse(since);
            List<MessageDto> messages = messageService.getMessagesAfterDate(conversationId, currentUser, sinceDate);
            return Response.ok(messages).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
}
