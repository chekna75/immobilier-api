package com.ditsolution.features.notification.dto;

import com.ditsolution.features.notification.entity.UserNotificationPreferencesEntity;
import lombok.Data;

import java.time.LocalTime;

@Data
public class UpdateNotificationPreferencesRequest {
    
    // Préférences générales
    private Boolean paymentReminders;
    private Boolean overdueAlerts;
    private Boolean paymentConfirmations;
    private Boolean newContracts;
    private Boolean newMessages;
    private Boolean listingStatusChanges;
    private Boolean favoriteUpdates;
    private Boolean systemUpdates;
    private Boolean marketingNotifications;
    
    // Préférences de rappels de paiement
    private Integer[] reminderDays;
    private LocalTime reminderTime;
    private UserNotificationPreferencesEntity.OverdueFrequency overdueFrequency;
    
    // Préférences de canaux
    private Boolean pushEnabled;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    
    // Heures silencieuses
    private Boolean quietHoursEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    
    // Fréquence des notifications
    private UserNotificationPreferencesEntity.NotificationFrequency notificationFrequency;
}
