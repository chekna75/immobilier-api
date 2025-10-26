package com.ditsolution.features.rental.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class FeeCalculationService {

    private static final Logger LOG = Logger.getLogger(FeeCalculationService.class);

    /**
     * Structure des frais par type de paiement
     */
    private static final Map<String, FeeStructure> FEE_STRUCTURES = Map.of(
        "rent", new FeeStructure(0.05, 2.50, 0.02),      // 5% plateforme, 2.50€ traitement, 2% assurance
        "deposit", new FeeStructure(0.03, 1.50, 0.01),   // 3% plateforme, 1.50€ traitement, 1% assurance
        "maintenance", new FeeStructure(0.02, 1.00, 0.005) // 2% plateforme, 1.00€ traitement, 0.5% assurance
    );

    /**
     * Calculer les frais automatiquement
     */
    public FeeCalculationResult calculateFees(BigDecimal amount, String paymentType) {
        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Le montant doit être positif");
            }

            FeeStructure fees = FEE_STRUCTURES.getOrDefault(paymentType, FEE_STRUCTURES.get("rent"));
            
            // Calculer les frais
            BigDecimal platformFeeAmount = amount.multiply(BigDecimal.valueOf(fees.platformFee))
                .setScale(2, RoundingMode.HALF_UP);
            BigDecimal insuranceFeeAmount = amount.multiply(BigDecimal.valueOf(fees.insuranceFee))
                .setScale(2, RoundingMode.HALF_UP);
            BigDecimal processingFeeAmount = BigDecimal.valueOf(fees.processingFee);
            
            BigDecimal totalFees = platformFeeAmount.add(processingFeeAmount).add(insuranceFeeAmount);
            BigDecimal totalAmount = amount.add(totalFees);

            // Créer le détail des frais
            Map<String, String> breakdown = new HashMap<>();
            breakdown.put("Montant de base", amount.setScale(2, RoundingMode.HALF_UP) + "€");
            breakdown.put("Frais de plateforme", platformFeeAmount + "€");
            breakdown.put("Frais de traitement", processingFeeAmount + "€");
            breakdown.put("Frais d'assurance", insuranceFeeAmount + "€");
            breakdown.put("Total des frais", totalFees + "€");
            breakdown.put("Montant total", totalAmount + "€");

            return new FeeCalculationResult(
                amount,
                platformFeeAmount,
                processingFeeAmount,
                insuranceFeeAmount,
                totalFees,
                totalAmount,
                breakdown,
                paymentType
            );

        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des frais", e);
            throw new RuntimeException("Erreur lors du calcul des frais: " + e.getMessage());
        }
    }

    /**
     * Calculer les frais pour un paiement fractionné
     */
    public SplitPaymentFeeResult calculateSplitPaymentFees(BigDecimal totalAmount, 
                                                          Integer depositPercentage) {
        try {
            BigDecimal depositAmount = totalAmount.multiply(BigDecimal.valueOf(depositPercentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal balanceAmount = totalAmount.subtract(depositAmount);

            FeeCalculationResult depositFees = calculateFees(depositAmount, "deposit");
            FeeCalculationResult balanceFees = calculateFees(balanceAmount, "rent");

            return new SplitPaymentFeeResult(
                totalAmount,
                depositPercentage,
                depositAmount,
                balanceAmount,
                depositFees,
                balanceFees
            );

        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des frais de paiement fractionné", e);
            throw new RuntimeException("Erreur lors du calcul des frais: " + e.getMessage());
        }
    }

    /**
     * Obtenir la structure des frais pour un type de paiement
     */
    public FeeStructure getFeeStructure(String paymentType) {
        return FEE_STRUCTURES.getOrDefault(paymentType, FEE_STRUCTURES.get("rent"));
    }

    /**
     * Mettre à jour la structure des frais (pour l'administration)
     */
    public void updateFeeStructure(String paymentType, FeeStructure newStructure) {
        // Dans une vraie application, vous pourriez vouloir persister ces valeurs
        // Pour l'instant, on garde les valeurs en mémoire
        LOG.info("Mise à jour de la structure des frais pour: " + paymentType);
    }

    /**
     * Structure des frais
     */
    public static class FeeStructure {
        public final double platformFee;    // Pourcentage des frais de plateforme
        public final double processingFee; // Frais de traitement fixes
        public final double insuranceFee;  // Pourcentage des frais d'assurance

        public FeeStructure(double platformFee, double processingFee, double insuranceFee) {
            this.platformFee = platformFee;
            this.processingFee = processingFee;
            this.insuranceFee = insuranceFee;
        }
    }

    /**
     * Résultat du calcul des frais
     */
    public static class FeeCalculationResult {
        public final BigDecimal baseAmount;
        public final BigDecimal platformFee;
        public final BigDecimal processingFee;
        public final BigDecimal insuranceFee;
        public final BigDecimal totalFees;
        public final BigDecimal totalAmount;
        public final Map<String, String> breakdown;
        public final String paymentType;

        public FeeCalculationResult(BigDecimal baseAmount, BigDecimal platformFee, 
                                  BigDecimal processingFee, BigDecimal insuranceFee,
                                  BigDecimal totalFees, BigDecimal totalAmount,
                                  Map<String, String> breakdown, String paymentType) {
            this.baseAmount = baseAmount;
            this.platformFee = platformFee;
            this.processingFee = processingFee;
            this.insuranceFee = insuranceFee;
            this.totalFees = totalFees;
            this.totalAmount = totalAmount;
            this.breakdown = breakdown;
            this.paymentType = paymentType;
        }
    }

    /**
     * Résultat du calcul des frais pour paiement fractionné
     */
    public static class SplitPaymentFeeResult {
        public final BigDecimal totalAmount;
        public final Integer depositPercentage;
        public final BigDecimal depositAmount;
        public final BigDecimal balanceAmount;
        public final FeeCalculationResult depositFees;
        public final FeeCalculationResult balanceFees;

        public SplitPaymentFeeResult(BigDecimal totalAmount, Integer depositPercentage,
                                   BigDecimal depositAmount, BigDecimal balanceAmount,
                                   FeeCalculationResult depositFees, FeeCalculationResult balanceFees) {
            this.totalAmount = totalAmount;
            this.depositPercentage = depositPercentage;
            this.depositAmount = depositAmount;
            this.balanceAmount = balanceAmount;
            this.depositFees = depositFees;
            this.balanceFees = balanceFees;
        }
    }
}
