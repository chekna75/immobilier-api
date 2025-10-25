package com.ditsolution.features.rental.service;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.repository.ListingRepository;
import com.ditsolution.features.rental.dto.*;
import com.ditsolution.features.rental.entity.RentalContractEntity;
import com.ditsolution.features.rental.entity.RentPaymentEntity;
import com.ditsolution.features.rental.repository.RentalContractRepository;
import com.ditsolution.features.rental.repository.RentPaymentRepository;
import com.ditsolution.features.rental.repository.PaymentTransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class RentalService {

    private static final Logger LOG = Logger.getLogger(RentalService.class);

    @Inject
    RentalContractRepository contractRepository;

    @Inject
    RentPaymentRepository paymentRepository;

    @Inject
    PaymentTransactionRepository transactionRepository;

    @Inject
    StripeService stripeService;

    @Inject
    RentalNotificationService notificationService;

    @Inject
    PdfGenerationService pdfGenerationService;

    @Inject
    ListingRepository listingRepository;

    @Transactional
    public RentalContractDto createContract(CreateContractRequest request, UserEntity owner) {
        // Vérifier que la propriété appartient au propriétaire
        ListingEntity property = listingRepository.findById(request.getPropertyId());
        if (property == null || !property.getOwner().id.equals(owner.id)) {
            throw new RuntimeException("Propriété non trouvée ou non autorisée");
        }

        // Vérifier que le locataire existe
        UserEntity tenant = UserEntity.findById(request.getTenantId());
        if (tenant == null || !tenant.role.equals(UserEntity.Role.TENANT)) {
            throw new RuntimeException("Locataire non trouvé");
        }

        // Créer le contrat
        RentalContractEntity contract = new RentalContractEntity();
        contract.setProperty(property);
        contract.setOwner(owner);
        contract.setTenant(tenant);
        contract.setMonthlyRent(request.getMonthlyRent());
        contract.setDeposit(request.getDeposit());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setPaymentDueDay(request.getPaymentDueDay());
        contract.setNotes(request.getNotes());
        contract.setStatus(RentalContractEntity.ContractStatus.ACTIVE);
        contract.persist();

        // Générer les premiers paiements
        generateMonthlyPayments(contract);

        return mapToContractDto(contract);
    }

    @Transactional
    public List<RentalContractDto> getOwnerContracts(UserEntity owner) {
        List<RentalContractEntity> contracts = contractRepository.findActiveContractsByOwner(owner);
        return contracts.stream()
                .map(this::mapToContractDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<RentalContractDto> getTenantContracts(UserEntity tenant) {
        List<RentalContractEntity> contracts = contractRepository.findActiveContractsByTenant(tenant);
        return contracts.stream()
                .map(this::mapToContractDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request, UserEntity user) {
        RentPaymentEntity payment = RentPaymentEntity.findById(request.getRentPaymentId());
        if (payment == null) {
            throw new RuntimeException("Paiement non trouvé");
        }

        // Vérifier que l'utilisateur est autorisé à payer
        if (!payment.getContract().getTenant().id.equals(user.id)) {
            throw new RuntimeException("Non autorisé à effectuer ce paiement");
        }

        if (!payment.getStatus().equals(RentPaymentEntity.PaymentStatus.PENDING)) {
            throw new RuntimeException("Ce paiement n'est pas en attente");
        }

        return stripeService.initiatePayment(request, payment);
    }

    @Transactional
    public void processPaymentCallback(String stripePaymentIntentId, String status) {
        com.ditsolution.features.rental.entity.PaymentTransactionEntity transaction = transactionRepository
                .findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée"));

        if ("succeeded".equals(status)) {
            // Marquer le paiement comme payé
            RentPaymentEntity payment = transaction.getRentPayment();
            payment.setStatus(RentPaymentEntity.PaymentStatus.PAID);
            payment.setPaidDate(LocalDate.now());
            payment.setPaymentMethod(transaction.getPaymentMethod().toString());
            payment.setCinetpayTransactionId(stripePaymentIntentId);
            payment.persist();

            // Générer le reçu PDF
            String receiptUrl = pdfGenerationService.generateReceipt(payment);
            payment.setReceiptUrl(receiptUrl);
            payment.persist();

            // Envoyer les notifications
            notificationService.sendPaymentConfirmation(payment);
            notificationService.sendPaymentNotificationToOwner(payment);

            LOG.info("Paiement traité avec succès: " + stripePaymentIntentId);
        } else {
            transaction.setStatus(com.ditsolution.features.rental.entity.PaymentTransactionEntity.TransactionStatus.FAILED);
            transaction.persist();
            LOG.warn("Paiement échoué: " + stripePaymentIntentId);
        }
    }

    @Transactional
    public RentalDashboardDto getOwnerDashboard(UserEntity owner) {
        List<RentalContractEntity> contracts = contractRepository.findActiveContractsByOwner(owner);
        
        BigDecimal totalMonthlyIncome = contracts.stream()
                .map(c -> c.getMonthlyRent())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RentPaymentEntity> allPayments = contracts.stream()
                .flatMap(c -> paymentRepository.findByContract(c).stream())
                .collect(Collectors.toList());

        BigDecimal totalCollected = allPayments.stream()
                .filter(p -> p.getStatus().equals(RentPaymentEntity.PaymentStatus.PAID))
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPending = allPayments.stream()
                .filter(p -> p.getStatus().equals(RentPaymentEntity.PaymentStatus.PENDING))
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOverdue = allPayments.stream()
                .filter(p -> p.getStatus().equals(RentPaymentEntity.PaymentStatus.PENDING) && p.getDueDate().isBefore(LocalDate.now()))
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        RentalDashboardDto dashboard = new RentalDashboardDto();
        dashboard.setTotalMonthlyIncome(totalMonthlyIncome);
        dashboard.setTotalCollected(totalCollected);
        dashboard.setTotalPending(totalPending);
        dashboard.setTotalOverdue(totalOverdue);
        dashboard.setActiveContracts(contracts.size());
        dashboard.setPendingPayments((int) allPayments.stream().filter(p -> p.getStatus().equals(RentPaymentEntity.PaymentStatus.PENDING)).count());
        dashboard.setOverduePaymentsCount((int) allPayments.stream().filter(p -> p.getStatus().equals(RentPaymentEntity.PaymentStatus.PENDING) && p.getDueDate().isBefore(LocalDate.now())).count());

        return dashboard;
    }

    @Transactional
    public void generateMonthlyPayments(RentalContractEntity contract) {
        LocalDate currentDate = LocalDate.now();
        LocalDate startDate = contract.getStartDate();
        
        // Générer les paiements pour les 12 prochains mois
        for (int i = 0; i < 12; i++) {
            LocalDate dueDate = startDate.plusMonths(i).withDayOfMonth(contract.getPaymentDueDay());
            
            // Vérifier si le paiement existe déjà
            boolean paymentExists = paymentRepository.find("contract = ?1 and dueDate = ?2", contract, dueDate).count() > 0;
            
            if (!paymentExists && dueDate.isAfter(currentDate.minusDays(1))) {
                RentPaymentEntity payment = new RentPaymentEntity();
                payment.setContract(contract);
                payment.setAmount(contract.getMonthlyRent());
                payment.setDueDate(dueDate);
                payment.setStatus(RentPaymentEntity.PaymentStatus.PENDING);
                payment.persist();
            }
        }
    }

    private RentalContractDto mapToContractDto(RentalContractEntity contract) {
        RentalContractDto dto = new RentalContractDto();
        dto.setId(contract.id);
        dto.setContractNumber(contract.getContractNumber());
        dto.setMonthlyRent(contract.getMonthlyRent());
        dto.setDeposit(contract.getDeposit());
        dto.setStartDate(contract.getStartDate());
        dto.setEndDate(contract.getEndDate());
        dto.setPaymentDueDay(contract.getPaymentDueDay());
        dto.setStatus(contract.getStatus().toString());
        dto.setNotes(contract.getNotes());
        dto.setCreatedAt(contract.getCreatedAt());
        dto.setUpdatedAt(contract.getUpdatedAt());
        
        // Mapper les paiements
        List<RentPaymentDto> paymentDtos = paymentRepository.findByContract(contract).stream()
                .map(this::mapToPaymentDto)
                .collect(Collectors.toList());
        dto.setPayments(paymentDtos);
        
        return dto;
    }

    private RentPaymentDto mapToPaymentDto(RentPaymentEntity payment) {
        RentPaymentDto dto = new RentPaymentDto();
        dto.setId(payment.id);
        dto.setContractId(payment.getContract().id);
        dto.setAmount(payment.getAmount());
        dto.setDueDate(payment.getDueDate());
        dto.setPaidDate(payment.getPaidDate());
        dto.setStatus(payment.getStatus().toString());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setTransactionId(payment.getTransactionId());
        dto.setCinetpayTransactionId(payment.getCinetpayTransactionId());
        dto.setPaymentReference(payment.getPaymentReference());
        dto.setLateFee(payment.getLateFee());
        dto.setNotes(payment.getNotes());
        dto.setReceiptUrl(payment.getReceiptUrl());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        return dto;
    }
}
