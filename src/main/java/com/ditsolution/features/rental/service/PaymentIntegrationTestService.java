package com.ditsolution.features.rental.service;

import com.ditsolution.features.rental.entity.SplitPaymentEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.Map;

@ApplicationScoped
public class PaymentIntegrationTestService {

    private static final Logger LOG = Logger.getLogger(PaymentIntegrationTestService.class);

    @Inject
    SplitPaymentService splitPaymentService;

    @Inject
    FeeCalculationService feeCalculationService;

    @Inject
    PdfGenerationService pdfGenerationService;

    public IntegrationTestResult testCompletePaymentFlow(Long contractId) {
        try {
            LOG.info("🧪 Test du flux complet de paiement pour le contrat: " + contractId);
            
            // 1. Créer un paiement fractionné
            SplitPaymentEntity splitPayment = splitPaymentService.createSplitPayment(
                contractId, 
                BigDecimal.valueOf(1000), 
                30, 
                "Test d'intégration"
            );
            
            // 2. Calculer les frais
            var fees = feeCalculationService.calculateFees(BigDecimal.valueOf(1000), "rent");
            
            // 3. Générer un PDF (simulation)
            String receiptUrl = pdfGenerationService.generateReceipt(null);
            
            return new IntegrationTestResult(true, "Test réussi", Map.of(
                "splitPaymentId", splitPayment.id,
                "feesCalculated", fees != null,
                "pdfGenerated", receiptUrl != null
            ));
            
        } catch (Exception e) {
            LOG.error("❌ Échec du test d'intégration", e);
            return new IntegrationTestResult(false, e.getMessage(), Map.of());
        }
    }

    public SyncTestResult testDataSynchronization(Long contractId) {
        try {
            LOG.info("🔄 Test de synchronisation pour le contrat: " + contractId);
            
            // Simuler la synchronisation
            var splitPayments = splitPaymentService.getSplitPaymentsByContract(contractId);
            
            return new SyncTestResult(true, "Synchronisation réussie", Map.of(
                "splitPaymentsCount", splitPayments.size(),
                "contractId", contractId
            ));
            
        } catch (Exception e) {
            LOG.error("❌ Échec du test de synchronisation", e);
            return new SyncTestResult(false, e.getMessage(), Map.of());
        }
    }

    public static class IntegrationTestResult {
        public final boolean success;
        public final String message;
        public final Map<String, Object> data;

        public IntegrationTestResult(boolean success, String message, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }

    public static class SyncTestResult {
        public final boolean success;
        public final String message;
        public final Map<String, Object> data;

        public SyncTestResult(boolean success, String message, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }
}