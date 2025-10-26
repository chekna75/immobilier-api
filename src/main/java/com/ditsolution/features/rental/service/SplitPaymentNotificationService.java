package com.ditsolution.features.rental.service;

import com.ditsolution.features.rental.entity.SplitPaymentEntity;
import com.ditsolution.features.rental.entity.SplitPaymentItemEntity;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class SplitPaymentNotificationService {

    private static final Logger LOG = Logger.getLogger(SplitPaymentNotificationService.class);

    // Service de notification simplifié pour les paiements fractionnés

    /**
     * Envoyer une notification pour un paiement fractionné créé
     */
    public void sendSplitPaymentCreatedNotification(SplitPaymentEntity splitPayment) {
        try {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "SPLIT_PAYMENT_CREATED");
            notificationData.put("splitPaymentId", splitPayment.id);
            notificationData.put("contractId", splitPayment.getContract().id);
            notificationData.put("totalAmount", splitPayment.getTotalAmount());
            notificationData.put("depositAmount", splitPayment.getDepositAmount());

            String title = "Paiement fractionné créé";
            String body = String.format(
                "Un paiement fractionné de %.2f€ a été créé pour votre contrat %s. " +
                "Acompte de %.2f€ à payer dans 7 jours.",
                splitPayment.getTotalAmount(),
                splitPayment.getContract().getContractNumber(),
                splitPayment.getDepositAmount()
            );

            // Log des notifications (à remplacer par un vrai service de notification)
            LOG.info("📱 Notification locataire: " + title + " - " + body);
            LOG.info("📱 Notification propriétaire: Nouveau paiement fractionné - " + 
                String.format("Votre locataire a créé un paiement fractionné de %.2f€ pour le contrat %s.",
                    splitPayment.getTotalAmount(),
                    splitPayment.getContract().getContractNumber()));

            LOG.info("✅ Notifications envoyées pour le paiement fractionné créé: " + splitPayment.id);

        } catch (Exception e) {
            LOG.error("❌ Erreur lors de l'envoi des notifications de création", e);
        }
    }

    /**
     * Envoyer une notification pour un paiement d'acompte réussi
     */
    public void sendDepositPaidNotification(SplitPaymentItemEntity depositItem) {
        try {
            SplitPaymentEntity splitPayment = depositItem.getSplitPayment();
            
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "DEPOSIT_PAID");
            notificationData.put("splitPaymentId", splitPayment.id);
            notificationData.put("itemId", depositItem.id);
            notificationData.put("amount", depositItem.getAmount());

            // Notification au locataire
            String tenantTitle = "Acompte payé avec succès";
            String tenantBody = String.format(
                "Votre acompte de %.2f€ a été payé avec succès. " +
                "Le solde de %.2f€ est maintenant disponible pour paiement.",
                depositItem.getAmount(),
                splitPayment.getBalanceAmount()
            );

            // Notification au propriétaire
            String ownerTitle = "Acompte reçu";
            String ownerBody = String.format(
                "Vous avez reçu l'acompte de %.2f€ pour le contrat %s.",
                depositItem.getAmount(),
                splitPayment.getContract().getContractNumber()
            );

            // Log des notifications (à remplacer par un vrai service de notification)
            LOG.info("📱 Notification locataire: " + tenantTitle + " - " + tenantBody);
            LOG.info("📱 Notification propriétaire: " + ownerTitle + " - " + ownerBody);

            LOG.info("✅ Notifications envoyées pour l'acompte payé: " + depositItem.id);

        } catch (Exception e) {
            LOG.error("❌ Erreur lors de l'envoi des notifications d'acompte", e);
        }
    }

    /**
     * Envoyer une notification pour un paiement de solde réussi
     */
    public void sendBalancePaidNotification(SplitPaymentItemEntity balanceItem) {
        try {
            SplitPaymentEntity splitPayment = balanceItem.getSplitPayment();
            
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "BALANCE_PAID");
            notificationData.put("splitPaymentId", splitPayment.id);
            notificationData.put("itemId", balanceItem.id);
            notificationData.put("amount", balanceItem.getAmount());

            // Notification au locataire
            String tenantTitle = "Paiement fractionné terminé";
            String tenantBody = String.format(
                "Félicitations ! Votre paiement fractionné de %.2f€ est maintenant complet. " +
                "Tous les documents sont disponibles dans votre espace.",
                splitPayment.getTotalAmount()
            );

            // Notification au propriétaire
            String ownerTitle = "Paiement fractionné complet";
            String ownerBody = String.format(
                "Le paiement fractionné de %.2f€ pour le contrat %s est maintenant complet.",
                splitPayment.getTotalAmount(),
                splitPayment.getContract().getContractNumber()
            );

            // Log des notifications (à remplacer par un vrai service de notification)
            LOG.info("📱 Notification locataire: " + tenantTitle + " - " + tenantBody);
            LOG.info("📱 Notification propriétaire: " + ownerTitle + " - " + ownerBody);

            LOG.info("✅ Notifications envoyées pour le solde payé: " + balanceItem.id);

        } catch (Exception e) {
            LOG.error("❌ Erreur lors de l'envoi des notifications de solde", e);
        }
    }

    /**
     * Programmer des rappels automatiques
     */
    public void schedulePaymentReminders(SplitPaymentEntity splitPayment) {
        try {
            // Rappel 3 jours avant l'échéance de l'acompte
            LocalDate depositReminderDate = splitPayment.getCreatedAt().toLocalDate().plusDays(4);
            
            // Rappel 3 jours avant l'échéance du solde
            LocalDate balanceReminderDate = splitPayment.getCreatedAt().toLocalDate().plusDays(27);

            // Programmer les rappels (dans une vraie app, utiliser un scheduler)
            LOG.info("📅 Rappels programmés pour le paiement fractionné: " + splitPayment.id);
            LOG.info("   - Rappel acompte: " + depositReminderDate);
            LOG.info("   - Rappel solde: " + balanceReminderDate);

        } catch (Exception e) {
            LOG.error("❌ Erreur lors de la programmation des rappels", e);
        }
    }
}