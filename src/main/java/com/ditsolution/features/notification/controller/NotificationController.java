package com.ditsolution.features.notification.controller;

import com.ditsolution.features.notification.dto.*;
import com.ditsolution.features.notification.service.NotificationService;
import com.ditsolution.shared.dto.PagedResponse;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/notifications")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationController {

    @Inject
    NotificationService notificationService;

    @Inject
    JsonWebToken jwt;

    /**
     * Enregistre un device token FCM pour l'utilisateur connecté
     */
    @POST
    @Path("/device-token")
    public Response registerDeviceToken(@Valid RegisterDeviceTokenRequest request) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            notificationService.registerDeviceToken(
                userId,
                request.getToken(),
                request.getPlatform(),
                request.getAppVersion(),
                request.getDeviceModel()
            );
            
            return Response.ok(Map.of("message", "Device token enregistré avec succès")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Récupère les notifications de l'utilisateur connecté
     */
    @GET
    public Response getUserNotifications(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            List<NotificationDto> notifications = notificationService.getUserNotifications(userId, page, size);
            long totalCount = notificationService.getUnreadNotificationCount(userId);
            
            PagedResponse<NotificationDto> response = new PagedResponse<>(
                notifications,
                totalCount,
                page,
                size
            );
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Marque une notification comme lue
     */
    @PUT
    @Path("/{notificationId}/read")
    public Response markNotificationAsRead(@PathParam("notificationId") UUID notificationId) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            notificationService.markNotificationAsRead(notificationId, userId);
            
            return Response.ok(Map.of("message", "Notification marquée comme lue")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Marque toutes les notifications comme lues
     */
    @PUT
    @Path("/read-all")
    public Response markAllNotificationsAsRead() {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            notificationService.markAllNotificationsAsRead(userId);
            
            return Response.ok(Map.of("message", "Toutes les notifications marquées comme lues")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Récupère le nombre de notifications non lues
     */
    @GET
    @Path("/unread-count")
    public Response getUnreadCount() {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            long count = notificationService.getUnreadNotificationCount(userId);
            
            return Response.ok(Map.of("unreadCount", count)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Envoie une notification (pour les tests ou l'admin)
     */
    @POST
    @Path("/send")
    public Response sendNotification(@Valid SendNotificationRequest request) {
        try {
            notificationService.sendNotificationToUser(request);
            
            return Response.ok(Map.of("message", "Notification envoyée avec succès")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
}

