package com.ditsolution.features.notification.controller;

import com.ditsolution.features.notification.dto.NotificationPreferencesDto;
import com.ditsolution.features.notification.dto.UpdateNotificationPreferencesRequest;
import com.ditsolution.features.notification.service.NotificationPreferencesService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;
import java.util.UUID;

@Path("/api/rental/notification-settings")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationPreferencesController {

    @Inject
    NotificationPreferencesService preferencesService;

    @Inject
    JsonWebToken jwt;

    /**
     * Récupère les préférences de notifications de l'utilisateur connecté
     */
    @GET
    public Response getUserPreferences() {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            NotificationPreferencesDto preferences = preferencesService.getUserPreferences(userId);
            
            return Response.ok(preferences).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erreur lors de la récupération des préférences: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Met à jour les préférences de notifications de l'utilisateur connecté
     */
    @PUT
    public Response updateUserPreferences(@Valid UpdateNotificationPreferencesRequest request) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            NotificationPreferencesDto updatedPreferences = preferencesService.updateUserPreferences(userId, request);
            
            return Response.ok(updatedPreferences).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Erreur lors de la mise à jour des préférences: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Réinitialise les préférences aux valeurs par défaut
     */
    @POST
    @Path("/reset")
    public Response resetToDefaults() {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            NotificationPreferencesDto defaultPreferences = preferencesService.resetToDefaults(userId);
            
            return Response.ok(defaultPreferences).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erreur lors de la réinitialisation: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Met à jour une préférence spécifique (pour les toggles rapides)
     */
    @PATCH
    @Path("/{preferenceKey}")
    public Response updateSinglePreference(
            @PathParam("preferenceKey") String preferenceKey,
            @QueryParam("value") String value) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            
            UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest();
            
            // Mapper la clé de préférence à la propriété correspondante
            switch (preferenceKey.toLowerCase()) {
                case "payment_reminders":
                    request.setPaymentReminders(Boolean.parseBoolean(value));
                    break;
                case "overdue_alerts":
                    request.setOverdueAlerts(Boolean.parseBoolean(value));
                    break;
                case "payment_confirmations":
                    request.setPaymentConfirmations(Boolean.parseBoolean(value));
                    break;
                case "new_contracts":
                    request.setNewContracts(Boolean.parseBoolean(value));
                    break;
                case "new_messages":
                    request.setNewMessages(Boolean.parseBoolean(value));
                    break;
                case "listing_status_changes":
                    request.setListingStatusChanges(Boolean.parseBoolean(value));
                    break;
                case "favorite_updates":
                    request.setFavoriteUpdates(Boolean.parseBoolean(value));
                    break;
                case "system_updates":
                    request.setSystemUpdates(Boolean.parseBoolean(value));
                    break;
                case "marketing_notifications":
                    request.setMarketingNotifications(Boolean.parseBoolean(value));
                    break;
                case "push_enabled":
                    request.setPushEnabled(Boolean.parseBoolean(value));
                    break;
                case "email_enabled":
                    request.setEmailEnabled(Boolean.parseBoolean(value));
                    break;
                case "sms_enabled":
                    request.setSmsEnabled(Boolean.parseBoolean(value));
                    break;
                case "quiet_hours_enabled":
                    request.setQuietHoursEnabled(Boolean.parseBoolean(value));
                    break;
                default:
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Préférence non reconnue: " + preferenceKey))
                        .build();
            }
            
            NotificationPreferencesDto updatedPreferences = preferencesService.updateUserPreferences(userId, request);
            
            return Response.ok(Map.of(
                "message", "Préférence mise à jour avec succès",
                "preference", preferenceKey,
                "value", value,
                "preferences", updatedPreferences
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Erreur lors de la mise à jour de la préférence: " + e.getMessage()))
                .build();
        }
    }
}
