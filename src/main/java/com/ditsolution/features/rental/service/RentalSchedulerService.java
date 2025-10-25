package com.ditsolution.features.rental.service;

import com.ditsolution.features.rental.entity.RentalContractEntity;
import com.ditsolution.features.rental.entity.RentPaymentEntity;
import com.ditsolution.features.rental.repository.RentalContractRepository;
import com.ditsolution.features.rental.repository.RentPaymentRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class RentalSchedulerService {

    private static final Logger LOG = Logger.getLogger(RentalSchedulerService.class);

    @Inject
    RentalContractRepository contractRepository;

    @Inject
    RentPaymentRepository paymentRepository;

    @Inject
    RentalService rentalService;

    @Inject
    RentalNotificationService notificationService;

    // Exécuter tous les jours à 9h00
    @Scheduled(cron = "0 0 9 * * ?")
    public void generateMonthlyPayments() {
        LOG.info("Début de la génération des paiements mensuels");
        
        try {
            List<RentalContractEntity> activeContracts = contractRepository.findActiveContracts();
            
            for (RentalContractEntity contract : activeContracts) {
                rentalService.generateMonthlyPayments(contract);
            }
            
            LOG.info("Génération des paiements mensuels terminée pour " + activeContracts.size() + " contrats");
        } catch (Exception e) {
            LOG.error("Erreur lors de la génération des paiements mensuels", e);
        }
    }

    // Exécuter tous les jours à 10h00 pour les rappels
    @Scheduled(cron = "0 0 10 * * ?")
    public void sendPaymentReminders() {
        LOG.info("Début de l'envoi des rappels de paiement");
        
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            List<RentPaymentEntity> paymentsDueTomorrow = paymentRepository.find("dueDate = ?1 and status = ?2", 
                    tomorrow, RentPaymentEntity.PaymentStatus.PENDING).list();
            
            if (!paymentsDueTomorrow.isEmpty()) {
                notificationService.sendBulkPaymentReminders(paymentsDueTomorrow);
                LOG.info("Rappels envoyés pour " + paymentsDueTomorrow.size() + " paiements");
            }
        } catch (Exception e) {
            LOG.error("Erreur lors de l'envoi des rappels", e);
        }
    }

    // Exécuter tous les jours à 11h00 pour les notifications de retard
    @Scheduled(cron = "0 0 11 * * ?")
    public void sendOverdueNotifications() {
        LOG.info("Début de l'envoi des notifications de retard");
        
        try {
            List<RentPaymentEntity> overduePayments = paymentRepository.findOverduePayments();
            
            if (!overduePayments.isEmpty()) {
                notificationService.sendBulkOverdueNotifications(overduePayments);
                LOG.info("Notifications de retard envoyées pour " + overduePayments.size() + " paiements");
            }
        } catch (Exception e) {
            LOG.error("Erreur lors de l'envoi des notifications de retard", e);
        }
    }

    // Exécuter tous les jours à minuit pour marquer les paiements en retard
    @Scheduled(cron = "0 0 0 * * ?")
    public void markOverduePayments() {
        LOG.info("Début de la mise à jour des paiements en retard");
        
        try {
            List<RentPaymentEntity> overduePayments = paymentRepository.findOverduePayments();
            
            for (RentPaymentEntity payment : overduePayments) {
                if (payment.getStatus().equals(RentPaymentEntity.PaymentStatus.PENDING)) {
                    // Calculer les frais de retard (par exemple 5% par mois de retard)
                    long daysOverdue = LocalDate.now().toEpochDay() - payment.getDueDate().toEpochDay();
                    if (daysOverdue > 0) {
                        double monthsOverdue = daysOverdue / 30.0;
                        payment.setLateFee(payment.getAmount().multiply(java.math.BigDecimal.valueOf(0.05 * monthsOverdue)));
                        payment.persist();
                    }
                }
            }
            
            LOG.info("Mise à jour des paiements en retard terminée pour " + overduePayments.size() + " paiements");
        } catch (Exception e) {
            LOG.error("Erreur lors de la mise à jour des paiements en retard", e);
        }
    }
}
