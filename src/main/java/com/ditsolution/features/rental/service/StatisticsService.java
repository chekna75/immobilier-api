package com.ditsolution.features.rental.service;

import com.ditsolution.features.rental.entity.RentPaymentEntity;
import com.ditsolution.features.rental.entity.RentalContractEntity;
import com.ditsolution.features.rental.repository.RentPaymentRepository;
import com.ditsolution.features.rental.repository.RentalContractRepository;
import com.ditsolution.features.rental.dto.StatisticsDto;
import com.ditsolution.features.rental.dto.PropertyStatisticsDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class StatisticsService {


    @Inject
    RentPaymentRepository paymentRepository;

    @Inject
    RentalContractRepository contractRepository;

    public StatisticsDto getStatistics(Long ownerId, String period) {
        LocalDate startDate = getStartDate(period);
        LocalDate endDate = LocalDate.now();

        // Récupérer tous les paiements de l'owner pour la période
        List<RentPaymentEntity> payments = paymentRepository.findByOwnerAndPeriod(ownerId, startDate, endDate);
        
        // Récupérer tous les contrats de l'owner
        List<RentalContractEntity> contracts = contractRepository.findByOwnerId(ownerId);

        StatisticsDto stats = new StatisticsDto();
        
        // Calculs des revenus
        stats.totalIncome = calculateTotalIncome(payments);
        stats.averageMonthlyIncome = calculateAverageMonthlyIncome(payments, period);
        stats.incomeTrend = calculateIncomeTrend(ownerId, period);
        
        // Calculs d'occupation
        stats.occupancyRate = calculateOccupancyRate(contracts);
        stats.occupancyTrend = calculateOccupancyTrend(ownerId, period);
        
        // Calculs de ponctualité
        stats.onTimePayments = calculateOnTimePayments(payments);
        stats.punctualityTrend = calculatePunctualityTrend(ownerId, period);
        stats.averageDelay = calculateAverageDelay(payments);
        
        // Statistiques par bien
        stats.properties = calculatePropertyStatistics(contracts, payments);
        
        // Résumé mensuel
        stats.bestMonth = findBestMonth(ownerId, period);
        stats.worstMonth = findWorstMonth(ownerId, period);
        
        return stats;
    }

    private LocalDate getStartDate(String period) {
        LocalDate now = LocalDate.now();
        return switch (period) {
            case "month" -> now.minusMonths(1);
            case "quarter" -> now.minusMonths(3);
            case "year" -> now.minusYears(1);
            default -> now.minusMonths(1);
        };
    }

    private BigDecimal calculateTotalIncome(List<RentPaymentEntity> payments) {
        return payments.stream()
                .filter(p -> p.getStatus() == RentPaymentEntity.PaymentStatus.PAID)
                .map(RentPaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAverageMonthlyIncome(List<RentPaymentEntity> payments, String period) {
        long months = switch (period) {
            case "month" -> 1;
            case "quarter" -> 3;
            case "year" -> 12;
            default -> 1;
        };
        
        BigDecimal totalIncome = calculateTotalIncome(payments);
        return totalIncome.divide(BigDecimal.valueOf(months), 2, java.math.RoundingMode.HALF_UP);
    }

    private Double calculateIncomeTrend(Long ownerId, String period) {
        // Comparer avec la période précédente
        LocalDate currentStart = getStartDate(period);
        LocalDate currentEnd = LocalDate.now();
        LocalDate previousStart = currentStart.minus(period.equals("month") ? 1 : period.equals("quarter") ? 3 : 12, 
                                                   period.equals("month") ? ChronoUnit.MONTHS : 
                                                   period.equals("quarter") ? ChronoUnit.MONTHS : ChronoUnit.MONTHS);
        LocalDate previousEnd = currentStart;

        List<RentPaymentEntity> currentPayments = paymentRepository.findByOwnerAndPeriod(ownerId, currentStart, currentEnd);
        List<RentPaymentEntity> previousPayments = paymentRepository.findByOwnerAndPeriod(ownerId, previousStart, previousEnd);

        BigDecimal currentIncome = calculateTotalIncome(currentPayments);
        BigDecimal previousIncome = calculateTotalIncome(previousPayments);

        if (previousIncome.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        return currentIncome.subtract(previousIncome)
                .divide(previousIncome, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private Double calculateOccupancyRate(List<RentalContractEntity> contracts) {
        if (contracts.isEmpty()) {
            return 0.0;
        }

        long activeContracts = contracts.stream()
                .filter(c -> c.getEndDate() == null || c.getEndDate().isAfter(LocalDate.now()))
                .count();

        return (double) activeContracts / contracts.size() * 100;
    }

    private Double calculateOccupancyTrend(Long ownerId, String period) {
        // Logique similaire pour calculer la tendance d'occupation
        return 0.0; // À implémenter selon les besoins
    }

    private Double calculateOnTimePayments(List<RentPaymentEntity> payments) {
        if (payments.isEmpty()) {
            return 0.0;
        }

        long onTimeCount = payments.stream()
                .filter(p -> p.getStatus() == RentPaymentEntity.PaymentStatus.PAID)
                .filter(p -> p.getPaidDate() != null && !p.getPaidDate().isAfter(p.getDueDate()))
                .count();

        return (double) onTimeCount / payments.size() * 100;
    }

    private Double calculatePunctualityTrend(Long ownerId, String period) {
        // Logique similaire pour calculer la tendance de ponctualité
        return 0.0; // À implémenter selon les besoins
    }

    private Double calculateAverageDelay(List<RentPaymentEntity> payments) {
        return payments.stream()
                .filter(p -> p.getStatus() == RentPaymentEntity.PaymentStatus.PAID)
                .filter(p -> p.getPaidDate() != null && p.getPaidDate().isAfter(p.getDueDate()))
                .mapToLong(p -> ChronoUnit.DAYS.between(p.getDueDate(), p.getPaidDate()))
                .average()
                .orElse(0.0);
    }

    private List<PropertyStatisticsDto> calculatePropertyStatistics(List<RentalContractEntity> contracts, 
                                                                   List<RentPaymentEntity> payments) {
        return contracts.stream()
                .map(contract -> {
                    PropertyStatisticsDto propStats = new PropertyStatisticsDto();
                    propStats.title = contract.getProperty().getTitle();
                    propStats.occupancyRate = contract.getEndDate() == null || contract.getEndDate().isAfter(LocalDate.now()) ? 100.0 : 0.0;
                    
                    // Calculer les revenus mensuels pour ce bien
                    List<RentPaymentEntity> propertyPayments = payments.stream()
                            .filter(p -> p.getContract().id.equals(contract.id))
                            .filter(p -> p.getStatus() == RentPaymentEntity.PaymentStatus.PAID)
                            .collect(Collectors.toList());
                    
                    propStats.monthlyIncome = calculateTotalIncome(propertyPayments);
                    propStats.paymentCount = propertyPayments.size();
                    propStats.averageDelay = calculateAverageDelay(propertyPayments);
                    
                    return propStats;
                })
                .collect(Collectors.toList());
    }

    private String findBestMonth(Long ownerId, String period) {
        // Logique pour trouver le meilleur mois
        return "Janvier 2024"; // Exemple
    }

    private String findWorstMonth(Long ownerId, String period) {
        // Logique pour trouver le pire mois
        return "Décembre 2023"; // Exemple
    }
}
