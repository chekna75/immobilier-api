package com.ditsolution.features.notification.entity;

import com.ditsolution.features.auth.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_notification_preferences")
@Data
@EqualsAndHashCode(callSuper = false)
public class UserNotificationPreferencesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    // Préférences générales
    @Column(name = "payment_reminders", nullable = false)
    private Boolean paymentReminders = true;

    @Column(name = "overdue_alerts", nullable = false)
    private Boolean overdueAlerts = true;

    @Column(name = "payment_confirmations", nullable = false)
    private Boolean paymentConfirmations = true;

    @Column(name = "new_contracts", nullable = false)
    private Boolean newContracts = true;

    @Column(name = "new_messages", nullable = false)
    private Boolean newMessages = true;

    @Column(name = "listing_status_changes", nullable = false)
    private Boolean listingStatusChanges = true;

    @Column(name = "favorite_updates", nullable = false)
    private Boolean favoriteUpdates = true;

    @Column(name = "system_updates", nullable = false)
    private Boolean systemUpdates = true;

    @Column(name = "marketing_notifications", nullable = false)
    private Boolean marketingNotifications = false;

    // Préférences de rappels de paiement
    @Column(name = "reminder_days", nullable = false, columnDefinition = "INTEGER[]")
    private Integer[] reminderDays = {1, 3, 7};

    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime = LocalTime.of(9, 0);

    @Enumerated(EnumType.STRING)
    @Column(name = "overdue_frequency", nullable = false)
    private OverdueFrequency overdueFrequency = OverdueFrequency.DAILY;

    // Préférences de canaux
    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private Boolean smsEnabled = false;

    // Heures silencieuses
    @Column(name = "quiet_hours_enabled", nullable = false)
    private Boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start", nullable = false)
    private LocalTime quietHoursStart = LocalTime.of(22, 0);

    @Column(name = "quiet_hours_end", nullable = false)
    private LocalTime quietHoursEnd = LocalTime.of(8, 0);

    // Fréquence des notifications
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_frequency", nullable = false)
    private NotificationFrequency notificationFrequency = NotificationFrequency.IMMEDIATE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum OverdueFrequency {
        DAILY, WEEKLY
    }

    public enum NotificationFrequency {
        IMMEDIATE, DAILY, WEEKLY
    }

    // Constructeurs
    public UserNotificationPreferencesEntity() {}

    public UserNotificationPreferencesEntity(UserEntity user) {
        this.user = user;
    }

    // Méthodes utilitaires
    public boolean isNotificationEnabled(NotificationEntity.NotificationType type) {
        switch (type) {
            case PAYMENT_REMINDER:
                return paymentReminders;
            case NEW_MESSAGE:
                return newMessages;
            case LISTING_STATUS_CHANGE:
                return listingStatusChanges;
            case FAVORITE_UPDATE:
                return favoriteUpdates;
            case SYSTEM_ANNOUNCEMENT:
                return systemUpdates;
            case NEW_LISTING_MATCH:
                return marketingNotifications;
            default:
                return true;
        }
    }

    public boolean isWithinQuietHours() {
        if (!quietHoursEnabled) {
            return false;
        }

        LocalTime now = LocalTime.now();
        
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            // Heures silencieuses dans la même journée (ex: 22h-08h)
            return now.isAfter(quietHoursStart) || now.isBefore(quietHoursEnd);
        } else {
            // Heures silencieuses qui traversent minuit (ex: 22h-08h)
            return now.isAfter(quietHoursStart) && now.isBefore(quietHoursEnd);
        }
    }

    public boolean shouldSendReminder(int daysUntilDue) {
        for (int day : reminderDays) {
            if (day == daysUntilDue) {
                return true;
            }
        }
        return false;
    }
}
