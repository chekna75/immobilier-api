package com.ditsolution.features.rental.service;

import com.ditsolution.features.rental.entity.RentalContractEntity;
import com.ditsolution.features.rental.entity.SplitPaymentEntity;
import com.ditsolution.features.rental.entity.SplitPaymentItemEntity;
import com.ditsolution.features.rental.repository.RentalContractRepository;
import com.ditsolution.features.rental.repository.SplitPaymentItemRepository;
import com.ditsolution.features.rental.repository.SplitPaymentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SplitPaymentService {

    private static final Logger LOG = Logger.getLogger(SplitPaymentService.class);

    @Inject
    SplitPaymentRepository splitPaymentRepository;

    @Inject
    SplitPaymentItemRepository splitPaymentItemRepository;

    @Inject
    RentalContractRepository rentalContractRepository;

    @Inject
    StripeService stripeService;

    @Inject
    PdfGenerationService pdfGenerationService;

    @Inject
    SplitPaymentNotificationService splitPaymentNotificationService;

    /**
     * Créer un paiement fractionné
     */
    @Transactional
    public SplitPaymentEntity createSplitPayment(Long contractId, BigDecimal totalAmount, 
                                                Integer depositPercentage, String description) {
        try {
            // Vérifier que le contrat existe
            RentalContractEntity contract = rentalContractRepository.findById(contractId);
            if (contract == null) {
                throw new RuntimeException("Contrat non trouvé");
            }

            // Vérifier qu'il n'y a pas déjà un paiement fractionné actif
            Optional<SplitPaymentEntity> existingSplit = splitPaymentRepository
                .findByContractIdAndStatus(contractId, SplitPaymentEntity.SplitPaymentStatus.PENDING);
            if (existingSplit.isPresent()) {
                throw new RuntimeException("Un paiement fractionné est déjà en cours pour ce contrat");
            }

            // Calculer les montants
            BigDecimal depositAmount = totalAmount.multiply(BigDecimal.valueOf(depositPercentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal balanceAmount = totalAmount.subtract(depositAmount);

            // Créer le paiement fractionné
            SplitPaymentEntity splitPayment = new SplitPaymentEntity();
            splitPayment.setContract(contract);
            splitPayment.setTotalAmount(totalAmount);
            splitPayment.setDepositPercentage(depositPercentage);
            splitPayment.setDepositAmount(depositAmount);
            splitPayment.setBalanceAmount(balanceAmount);
            splitPayment.setDescription(description);
            splitPayment.setStatus(SplitPaymentEntity.SplitPaymentStatus.PENDING);
            splitPayment.persist();

            // Créer les éléments de paiement
            createPaymentItems(splitPayment);

            // Envoyer les notifications
            splitPaymentNotificationService.sendSplitPaymentCreatedNotification(splitPayment);
            
            // Programmer les rappels
            splitPaymentNotificationService.schedulePaymentReminders(splitPayment);

            // Tracker l'événement
            LOG.info("Paiement fractionné créé avec succès - ID: " + splitPayment.id);

            LOG.info("Paiement fractionné créé: " + splitPayment.id + " pour le contrat: " + contractId);
            return splitPayment;

        } catch (Exception e) {
            LOG.error("Erreur lors de la création du paiement fractionné", e);
            throw new RuntimeException("Erreur lors de la création du paiement fractionné: " + e.getMessage());
        }
    }

    /**
     * Créer les éléments de paiement (acompte et solde)
     */
    private void createPaymentItems(SplitPaymentEntity splitPayment) {
        // Créer l'acompte
        SplitPaymentItemEntity depositItem = new SplitPaymentItemEntity();
        depositItem.setSplitPayment(splitPayment);
        depositItem.setPaymentType(SplitPaymentItemEntity.PaymentType.DEPOSIT);
        depositItem.setAmount(splitPayment.getDepositAmount());
        depositItem.setDueDate(LocalDate.now().plusDays(7)); // 7 jours pour l'acompte
        depositItem.setStatus(SplitPaymentItemEntity.PaymentItemStatus.PENDING);
        depositItem.persist();

        // Créer le solde
        SplitPaymentItemEntity balanceItem = new SplitPaymentItemEntity();
        balanceItem.setSplitPayment(splitPayment);
        balanceItem.setPaymentType(SplitPaymentItemEntity.PaymentType.BALANCE);
        balanceItem.setAmount(splitPayment.getBalanceAmount());
        balanceItem.setDueDate(LocalDate.now().plusDays(30)); // 30 jours pour le solde
        balanceItem.setStatus(SplitPaymentItemEntity.PaymentItemStatus.PENDING);
        balanceItem.persist();
    }

    /**
     * Traiter le paiement d'un élément
     */
    @Transactional
    public void processPaymentItem(Long itemId, String stripePaymentIntentId) {
        try {
            SplitPaymentItemEntity item = splitPaymentItemRepository.findById(itemId);
            if (item == null) {
                throw new RuntimeException("Élément de paiement non trouvé");
            }

            // Mettre à jour l'élément
            item.setStatus(SplitPaymentItemEntity.PaymentItemStatus.PAID);
            item.setPaidDate(LocalDate.now());
            item.setStripePaymentIntentId(stripePaymentIntentId);
            item.persist();

            // Générer le reçu
            String receiptUrl = pdfGenerationService.generateSplitPaymentReceipt(item);
            item.setReceiptUrl(receiptUrl);
            item.persist();

            // Mettre à jour le statut du paiement fractionné
            updateSplitPaymentStatus(item.getSplitPayment());

            // Envoyer les notifications spécifiques
            if (item.getPaymentType() == SplitPaymentItemEntity.PaymentType.DEPOSIT) {
                splitPaymentNotificationService.sendDepositPaidNotification(item);
                LOG.info("Acompte payé - ID: " + item.id);
            } else {
                splitPaymentNotificationService.sendBalancePaidNotification(item);
                LOG.info("Solde payé - ID: " + item.id);
            }

            LOG.info("Paiement traité pour l'élément: " + itemId);

        } catch (Exception e) {
            LOG.error("Erreur lors du traitement du paiement", e);
            throw new RuntimeException("Erreur lors du traitement du paiement: " + e.getMessage());
        }
    }

    /**
     * Mettre à jour le statut du paiement fractionné
     */
    private void updateSplitPaymentStatus(SplitPaymentEntity splitPayment) {
        List<SplitPaymentItemEntity> items = splitPaymentItemRepository
            .findBySplitPaymentId(splitPayment.id);

        boolean depositPaid = items.stream()
            .anyMatch(item -> item.getPaymentType() == SplitPaymentItemEntity.PaymentType.DEPOSIT 
                          && item.getStatus() == SplitPaymentItemEntity.PaymentItemStatus.PAID);

        boolean balancePaid = items.stream()
            .anyMatch(item -> item.getPaymentType() == SplitPaymentItemEntity.PaymentType.BALANCE 
                          && item.getStatus() == SplitPaymentItemEntity.PaymentItemStatus.PAID);

        if (balancePaid) {
            splitPayment.setStatus(SplitPaymentEntity.SplitPaymentStatus.COMPLETED);
        } else if (depositPaid) {
            splitPayment.setStatus(SplitPaymentEntity.SplitPaymentStatus.DEPOSIT_PAID);
        }

        splitPayment.persist();
    }

    /**
     * Obtenir les paiements fractionnés d'un contrat
     */
    public List<SplitPaymentEntity> getSplitPaymentsByContract(Long contractId) {
        return splitPaymentRepository.findByContractId(contractId);
    }

    /**
     * Obtenir un paiement fractionné par ID
     */
    public Optional<SplitPaymentEntity> getSplitPaymentById(Long splitPaymentId) {
        return splitPaymentRepository.findByIdOptional(splitPaymentId);
    }

    /**
     * Annuler un paiement fractionné
     */
    @Transactional
    public void cancelSplitPayment(Long splitPaymentId) {
        try {
            SplitPaymentEntity splitPayment = splitPaymentRepository.findById(splitPaymentId);
            if (splitPayment == null) {
                throw new RuntimeException("Paiement fractionné non trouvé");
            }

            // Annuler tous les éléments non payés
            List<SplitPaymentItemEntity> items = splitPaymentItemRepository
                .findBySplitPaymentId(splitPaymentId);
            
            for (SplitPaymentItemEntity item : items) {
                if (item.getStatus() == SplitPaymentItemEntity.PaymentItemStatus.PENDING) {
                    item.setStatus(SplitPaymentItemEntity.PaymentItemStatus.CANCELLED);
                    item.persist();
                }
            }

            splitPayment.setStatus(SplitPaymentEntity.SplitPaymentStatus.CANCELLED);
            splitPayment.persist();

            LOG.info("Paiement fractionné annulé: " + splitPaymentId);

        } catch (Exception e) {
            LOG.error("Erreur lors de l'annulation du paiement fractionné", e);
            throw new RuntimeException("Erreur lors de l'annulation: " + e.getMessage());
        }
    }

    /**
     * Obtenir les statistiques des paiements fractionnés
     */
    public SplitPaymentStatistics getSplitPaymentStatistics(Long contractId) {
        List<SplitPaymentEntity> splitPayments = splitPaymentRepository.findByContractId(contractId);
        
        long totalSplitPayments = splitPayments.size();
        long completedPayments = splitPayments.stream()
            .mapToLong(sp -> sp.getStatus() == SplitPaymentEntity.SplitPaymentStatus.COMPLETED ? 1 : 0)
            .sum();
        long pendingPayments = splitPayments.stream()
            .mapToLong(sp -> sp.getStatus() == SplitPaymentEntity.SplitPaymentStatus.PENDING ? 1 : 0)
            .sum();

        return new SplitPaymentStatistics(totalSplitPayments, completedPayments, pendingPayments);
    }

    public static class SplitPaymentStatistics {
        public final long totalSplitPayments;
        public final long completedPayments;
        public final long pendingPayments;

        public SplitPaymentStatistics(long totalSplitPayments, long completedPayments, long pendingPayments) {
            this.totalSplitPayments = totalSplitPayments;
            this.completedPayments = completedPayments;
            this.pendingPayments = pendingPayments;
        }
    }
}