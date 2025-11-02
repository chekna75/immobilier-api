package com.ditsolution.features.rental.service;

import com.ditsolution.features.rental.entity.RentPaymentEntity;
import com.ditsolution.features.notification.dto.SendNotificationRequest;
import com.ditsolution.features.notification.entity.NotificationEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RentalNotificationService {

    private static final Logger LOG = Logger.getLogger(RentalNotificationService.class);

    @Inject
    com.ditsolution.features.notification.service.NotificationService baseNotificationService;

    public void sendPaymentConfirmation(RentPaymentEntity payment) {
        try {
            String title = "Paiement confirmé";
            String body = String.format("Votre paiement de loyer de %.2f EUR a été confirmé pour la période %s", 
                    payment.getAmount(), 
                    payment.getDueDate().format(DateTimeFormatter.ofPattern("MM/yyyy")));

            Map<String, String> metadata = new HashMap<>();
            metadata.put("type", "PAYMENT_CONFIRMATION");
            metadata.put("payment_id", payment.id.toString());
            metadata.put("receipt_url", payment.getReceiptUrl() != null ? payment.getReceiptUrl() : "");

            SendNotificationRequest request = new SendNotificationRequest();
            request.setUserId(payment.getContract().getTenant().id);
            request.setType(NotificationEntity.NotificationType.PAYMENT_REMINDER);
            request.setTitle(title);
            request.setBody(body);
            request.setData(metadata);
            request.setRelatedEntityType("RENT_PAYMENT");
            request.setRelatedEntityId(payment.id.toString());

            baseNotificationService.sendNotificationToUser(request);

            LOG.info("Notification de confirmation envoyée pour le paiement: " + payment.id);
        } catch (Exception e) {
            LOG.error("Erreur lors de l'envoi de la notification de confirmation", e);
        }
    }

    public void sendPaymentNotificationToOwner(RentPaymentEntity payment) {
        try {
            String title = "Nouveau paiement reçu";
            String body = String.format("Vous avez reçu un paiement de loyer de %.2f EUR de %s %s", 
                    payment.getAmount(),
                    payment.getContract().getTenant().firstName,
                    payment.getContract().getTenant().lastName);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("type", "PAYMENT_RECEIVED");
            metadata.put("payment_id", payment.id.toString());
            metadata.put("tenant_name", payment.getContract().getTenant().firstName + " " + payment.getContract().getTenant().lastName);

            SendNotificationRequest request = new SendNotificationRequest();
            request.setUserId(payment.getContract().getOwner().id);
            request.setType(NotificationEntity.NotificationType.SYSTEM_ANNOUNCEMENT);
            request.setTitle(title);
            request.setBody(body);
            request.setData(metadata);
            request.setRelatedEntityType("RENT_PAYMENT");
            request.setRelatedEntityId(payment.id.toString());

            baseNotificationService.sendNotificationToUser(request);

            LOG.info("Notification au propriétaire envoyée pour le paiement: " + payment.id);
        } catch (Exception e) {
            LOG.error("Erreur lors de l'envoi de la notification au propriétaire", e);
        }
    }

    public void sendPaymentReminder(RentPaymentEntity payment) {
        try {
            String title = "Rappel de paiement";
            String body = String.format("N'oubliez pas de payer votre loyer de %.2f EUR avant le %s", 
                    payment.getAmount(),
                    payment.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            Map<String, String> metadata = new HashMap<>();
            metadata.put("type", "PAYMENT_REMINDER");
            metadata.put("payment_id", payment.id.toString());
            metadata.put("due_date", payment.getDueDate().toString());

            SendNotificationRequest request = new SendNotificationRequest();
            request.setUserId(payment.getContract().getTenant().id);
            request.setType(NotificationEntity.NotificationType.PAYMENT_REMINDER);
            request.setTitle(title);
            request.setBody(body);
            request.setData(metadata);
            request.setRelatedEntityType("RENT_PAYMENT");
            request.setRelatedEntityId(payment.id.toString());

            baseNotificationService.sendNotificationToUser(request);

            LOG.info("Rappel de paiement envoyé pour: " + payment.id);
        } catch (Exception e) {
            LOG.error("Erreur lors de l'envoi du rappel de paiement", e);
        }
    }

    public void sendOverdueNotification(RentPaymentEntity payment) {
        try {
            String title = "Paiement en retard";
            String body = String.format("Votre loyer de %.2f EUR est en retard depuis le %s", 
                    payment.getAmount(),
                    payment.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            Map<String, String> metadata = new HashMap<>();
            metadata.put("type", "PAYMENT_OVERDUE");
            metadata.put("payment_id", payment.id.toString());
            metadata.put("days_overdue", String.valueOf(LocalDate.now().toEpochDay() - payment.getDueDate().toEpochDay()));

            SendNotificationRequest request = new SendNotificationRequest();
            request.setUserId(payment.getContract().getTenant().id);
            request.setType(NotificationEntity.NotificationType.PAYMENT_REMINDER);
            request.setTitle(title);
            request.setBody(body);
            request.setData(metadata);
            request.setRelatedEntityType("RENT_PAYMENT");
            request.setRelatedEntityId(payment.id.toString());

            baseNotificationService.sendNotificationToUser(request);

            LOG.info("Notification de retard envoyée pour: " + payment.id);
        } catch (Exception e) {
            LOG.error("Erreur lors de l'envoi de la notification de retard", e);
        }
    }

    public void sendBulkPaymentReminders(List<RentPaymentEntity> payments) {
        for (RentPaymentEntity payment : payments) {
            sendPaymentReminder(payment);
        }
    }

    public void sendBulkOverdueNotifications(List<RentPaymentEntity> payments) {
        for (RentPaymentEntity payment : payments) {
            sendOverdueNotification(payment);
        }
    }
}
